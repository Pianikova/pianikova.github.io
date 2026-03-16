/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ProcessRunner
    implements IProcessRunner
{
    private static final String CONSOLE_CHARSET;
    private final ILog log;
    private final ExecutorService executor;

    static
    {
        CONSOLE_CHARSET = detectConsoleCharset();
    }

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
        List<String> args, Long timeout, TimeUnit timeUnit, Integer maxLines)
    {
        Preconditions.checkNotNull(executable);
        if (timeout == null)
        {
            timeout = 15L;
        }

        if (maxLines == null)
        {
            maxLines = 1000; // Default limit
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
        var stdOutFuture = readStreamAsync(process.getInputStream(), maxLines);
        var stdErrFuture = readStreamAsync(process.getErrorStream(), maxLines);

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
            stdOutFuture.thenCombine(stdErrFuture, (stdOutResult, stdErrResult) -> {
                var result = new ProcessResult();
                result.stdOut = stdOutResult.content;
                result.stdErr = stdErrResult.content;
                result.stdOutTruncated = stdOutResult.truncated;
                result.stdErrTruncated = stdErrResult.truncated;
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
     * @param maxLines Maximum number of lines to read
     * @return Future containing stream read result with content and truncation flag
     */
    private CompletableFuture<StreamReadResult> readStreamAsync(InputStream inputStream, int maxLines)
    {
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                java.io.BufferedReader reader =
                    new java.io.BufferedReader(new InputStreamReader(inputStream, CONSOLE_CHARSET));

                StringBuilder lineBuffer = new StringBuilder();
                int lineCount = 0;
                boolean truncated = false;
                String line;

                // Read line by line until EOF or line limit reached
                while ((line = reader.readLine()) != null && lineCount < maxLines)
                {
                    if (lineCount > 0)
                    {
                        lineBuffer.append('\n');
                    }
                    lineBuffer.append(line);
                    lineCount++;
                }

                // Check if we stopped due to line limit
                if (line != null)
                {
                    truncated = true;
                }

                String content = lineBuffer.toString();
                return new StreamReadResult(content, truncated);
            }
            catch (IOException e)
            {
                // Wrap checked exception for CompletableFuture
                throw new UncheckedIOException("Error reading process stream", e); //$NON-NLS-1$
            }
        }, executor);
    }

    @SuppressWarnings("nls")
    private static String detectConsoleCharset()
    {
        var os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win"))
        {
            // On Windows, try Java 10+ console charset
            try
            {
                var charsetMethod = java.io.Console.class.getMethod("charset");
                var console = System.console();
                if (console != null)
                {
                    var charset = (java.nio.charset.Charset)charsetMethod.invoke(console);
                    return charset.name();
                }
            }
            catch (Exception e)
            {
                // Not Java 10+ or no console
            }

            // Try sun.stdout.encoding
            String encoding = System.getProperty("sun.stdout.encoding");
            if (encoding != null && !encoding.isEmpty())
            {
                return encoding;
            }

            // Fallback based on language
            String lang = System.getProperty("user.language", "en").toLowerCase();
            return lang.equals("ru") ? "CP866" : "CP437";
        }

        // Unix/Linux
        return "UTF-8";
    }
}