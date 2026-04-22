/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.google.common.base.Preconditions;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;

/**
 * Represents a JShell REPL session.
 */
class JShellSession
    implements IJShellSession
{
    private static final int MAX_EXECUTION_HISTORY_SIZE = 4096;
	private final int sessionId;
	private final JShell shell;
	private final ByteArrayOutputStream outBuffer;
	private final ByteArrayOutputStream errBuffer;
    private final IRestrictedTypesValidator restrictedTypesValidator;
    private final Set<IJShellBindingProvider> bindingProviders;
    private final List<String> executionHistory = new ArrayList<>();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private ArrayList<String> cachedAvailableBindings;

    JShellSession(int sessionId, JShell shell, ByteArrayOutputStream outBuffer, ByteArrayOutputStream errBuffer,
        IRestrictedTypesValidator restrictedTypesValidator, Set<IJShellBindingProvider> bindingProviders)
	{
        Preconditions.checkNotNull(shell);
        Preconditions.checkNotNull(outBuffer);
        Preconditions.checkNotNull(errBuffer);
        Preconditions.checkNotNull(restrictedTypesValidator);
        Preconditions.checkNotNull(bindingProviders);

        this.sessionId = sessionId;
		this.shell = shell;
		this.outBuffer = outBuffer;
		this.errBuffer = errBuffer;
        this.restrictedTypesValidator = restrictedTypesValidator;
        this.bindingProviders = bindingProviders;
        this.cachedAvailableBindings = buildAvailableBindings();
	}

    /**
     * Builds the list of available binding names from all providers.
     */
    private ArrayList<String> buildAvailableBindings()
    {
        var bindings = new ArrayList<String>();
        for (var provider : bindingProviders)
        {
            var descriptions = provider.getBindings();
            bindings.addAll(descriptions.keySet());
        }

        return bindings;
    }

    /**
     * Adds source to execution history, maintaining maximum size.
     */
    private void addToExecutionHistory(String source)
    {
        executionHistory.add(source);
        if (executionHistory.size() > MAX_EXECUTION_HISTORY_SIZE)
        {
            executionHistory.remove(0);
        }
    }

    /**
     * Populates session result fields (available bindings and execution history).
     */
    private void populateSessionResult(SessionResult result)
    {
        result.availableBindings = new ArrayList<>(cachedAvailableBindings);
        result.executionHistory = new ArrayList<>(executionHistory);
    }

	@Override
    public int getSessionId()
	{
		return sessionId;
	}

    @SuppressWarnings({ "nls", "incomplete-switch" })
    @Override
    public JShellExecutionResult execute(String code)
	{
        if (isClosed.get())
        {
            throw new ToolException("Session is closed. Cannot execute code.", null, ToolErrorType.RETRYABLE);
        }

        var result = new JShellExecutionResult();
        result.sessionId = sessionId;
        result.compilationErrors = new ArrayList<>();
        result.runtimeErrors = new ArrayList<>();
        if (restrictedTypesValidator != null)
        {
            try
            {
                restrictedTypesValidator.validate(code);
            }
            catch (ToolException e)
            {
                // Add as compilation error
                var error = new CompilationError();
                error.isError = true;
                error.message = e.getMessage();
                result.compilationErrors.add(error);
                return result;
            }
        }

        // Clear buffers
        outBuffer.reset();
        errBuffer.reset();

        try
        {
            // Split code into individual completions using analyzeCompletion
            var analysis = shell.sourceCodeAnalysis();
            var remaining = code;
            while (result.compilationErrors.isEmpty() && remaining != null && !remaining.isBlank())
            {
                var completion = analysis.analyzeCompletion(remaining);
                String source;
                if (completion.completeness().isComplete())
                {
                    source = completion.source();
                    remaining = completion.remaining();
                }
                else
                {
                    source = completion.remaining();
                }

                var events = shell.eval(source);
                if (!events.isEmpty())
                {
                    for (SnippetEvent event : events)
                    {
                        switch (event.status())
                        {
                        case REJECTED:
                            // Get structured diagnostics for compilation errors
                            var diagnostics = shell.diagnostics(event.snippet()).collect(Collectors.toList());
                            if (!diagnostics.isEmpty())
                            {
                                for (var diag : diagnostics)
                                {
                                    var error = new CompilationError();
                                    error.isError = diag.isError();
                                    error.code = diag.getCode();
                                    error.message = diag.getMessage(null);
                                    error.position = diag.getPosition();
                                    error.startPosition = diag.getStartPosition();
                                    error.endPosition = diag.getEndPosition();

                                    // Determine the error type based on error code
                                    var errorCode = diag.getCode();
                                    if (errorCode != null)
                                    {
                                        error.isResolutionError = errorCode.startsWith("compiler.err.cant.resolve")
                                            || "compiler.err.cant.apply.symbol".equals(errorCode);
                                        error.isUnreachableError = "compiler.err.unreachable.stmt".equals(errorCode);
                                        error.isNotAStatementError = "compiler.err.not.stmt".equals(errorCode);
                                    }

                                    result.compilationErrors.add(error);
                                }
                            }
                            break;

                        case VALID:
                            addToExecutionHistory(source);
                            // Check for runtime exceptions
                            if (event.exception() != null)
                            {
                                var exception = event.exception();
                                var error = new RuntimeError();
                                error.exceptionType = exception.getClass().getName();
                                error.message = exception.getMessage();

                                var stackTrace = new java.io.StringWriter();
                                exception.printStackTrace(new java.io.PrintWriter(stackTrace));
                                error.stackTrace = stackTrace.toString();

                                result.runtimeErrors.add(error);
                            }
                            break;
                        }
                    }
                }
            }
        }
        catch (OutOfMemoryError e)
        {
            var error = new RuntimeError();
            error.exceptionType = e.getClass().getName();
            error.message = "Out of memory during code execution. The code may have allocated too much memory.";
            error.stackTrace = e.getMessage();
            result.runtimeErrors.add(error);
            result.stdOut = outBuffer.toString();
            result.stdErr = errBuffer.toString();
            populateSessionResult(result);
            return result;
        }
        catch (ThreadDeath e)
        {
            throw e;
        }
        catch (Throwable e)
        {
            var error = new RuntimeError();
            error.exceptionType = e.getClass().getName();
            error.message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            var stackTrace = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(stackTrace));
            error.stackTrace = stackTrace.toString();
            result.runtimeErrors.add(error);
            result.stdOut = outBuffer.toString();
            result.stdErr = errBuffer.toString();
            populateSessionResult(result);
            return result;
        }

        result.stdOut = outBuffer.toString();
        result.stdErr = errBuffer.toString();

        // Fill session result fields
        populateSessionResult(result);

        return result;
	}

	@Override
    public List<String> getExecutionHistory()
    {
        return new ArrayList<>(executionHistory);
    }

    @Override
    public SessionResult getSessionResult()
    {
        var result = new SessionResult();
        result.sessionId = sessionId;
        populateSessionResult(result);
        return result;
    }

    @Override
    public void close()
	{
        if (!isClosed.getAndSet(true))
        {
            try
            {
                shell.close();
                JShellObjectBridge.releaseSession(sessionId);
            }
            finally
            {
                executionHistory.clear();
            }
        }
	}
}
