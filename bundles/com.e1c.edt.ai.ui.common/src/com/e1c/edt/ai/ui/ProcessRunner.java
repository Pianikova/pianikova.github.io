/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.assistent.model.ProcessResult;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ProcessRunner
    implements IProcessRunner
{
    private final ILog log;
    private final ExecutorService executor;

    @Inject
    public ProcessRunner(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    @SuppressWarnings("nls")
    public CompletableFuture<Optional<ProcessResult>> executeProcess(String executable, String workingDirectory,
        List<String> args, Long timeout, TimeUnit timeUnit)
    {
        Preconditions.checkNotNull(executable);
        if (timeout == null)
        {
            timeout = 15L;
        }

        Process process;
        try
        {
            // Prepare and start the process
            var processBuilder = new ProcessBuilder();
            var command = processBuilder.command();
            command.add(executable);

            if (args != null && !args.isEmpty())
            {
                command.addAll(args);
            }

            if (workingDirectory != null)
            {
                processBuilder.directory(new File(workingDirectory));
            }

            process = processBuilder.start();
        }
        catch (IOException e)
        {
            // Immediately return failed future if process can't start
            log.logError(e);
            return CompletableFuture.failedFuture(e);
        }

        // Asynchronously read output streams
        var stdOutFuture = readStreamAsync(process.getInputStream());
        var stdErrFuture = readStreamAsync(process.getErrorStream());

        // Wait for process completion asynchronously
        var exitCodeFuture = CompletableFuture.supplyAsync(() -> {
            try
            {
                // Block until process terminates
                return process.waitFor();
            }
            catch (InterruptedException e)
            {
                // Preserve interrupt status and propagate error
                Thread.currentThread().interrupt();
                throw new RuntimeException("Process execution interrupted", e);
            }
        }, executor);

        // Combine all results: exit code + stdout + stderr
        var resultFuture = exitCodeFuture.thenCombineAsync(
            // Combine stdout and stderr first
            stdOutFuture.thenCombine(stdErrFuture, (stdOut, stdErr) -> {
                var result = new ProcessResult();
                result.stdOut = stdOut;
                result.stdErr = stdErr;
                return result;
            }),
            // Then combine with exit code to create final result
            (exitCode, result) -> {
                result.exitCode = exitCode;
                return Optional.of(result);
            }, executor).exceptionally(ex -> {
                // Unified error handling
                log.logError(ex);

                // Ensure process termination on error
                if (process.isAlive())
                {
                    process.destroyForcibly();
                }

                // Cancel stream reading tasks
                stdOutFuture.cancel(true);
                stdErrFuture.cancel(true);

                // Return error result
                return Optional.empty();
            });

        // Apply timeout
        return resultFuture.orTimeout(timeout, timeUnit).exceptionally(ex -> {
            if (ex instanceof TimeoutException)
            {
                log.logError(ex);

                // Ensure process termination on timeout
                if (process.isAlive())
                {
                    process.destroyForcibly();
                }

                // Cancel stream reading tasks
                stdOutFuture.cancel(true);
                stdErrFuture.cancel(true);
            }
            return Optional.empty();
        });
    }


    /**
     * Asynchronously reads content from an input stream
     * @param inputStream Stream to read from
     * @return Future containing stream content as String
     */
    private CompletableFuture<String> readStreamAsync(InputStream inputStream)
    {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStreamReader isr = new InputStreamReader(inputStream);
                StringWriter sw = new StringWriter())
            {

                char[] buffer = new char[1024];
                int bytesRead;
                // Read stream until EOF
                while ((bytesRead = isr.read(buffer)) != -1)
                {
                    sw.write(buffer, 0, bytesRead);
                }

                return sw.toString();
            }
            catch (IOException e)
            {
                // Wrap checked exception for CompletableFuture
                throw new UncheckedIOException("Error reading process stream", e); //$NON-NLS-1$
            }
        }, executor);
    }
}