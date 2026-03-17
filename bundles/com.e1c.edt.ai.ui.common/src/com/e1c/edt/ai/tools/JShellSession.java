/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.stream.Collectors;

import jdk.jshell.Diag;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;

/**
 * Represents a JShell REPL session.
 */
class JShellSession
    implements IJShellSession
{
	private final String sessionId;
	private final JShell shell;
	private final ByteArrayOutputStream outBuffer;
	private final ByteArrayOutputStream errBuffer;

	JShellSession(JShell shell, ByteArrayOutputStream outBuffer, ByteArrayOutputStream errBuffer)
	{
		this.sessionId = UUID.randomUUID().toString();
		this.shell = shell;
		this.outBuffer = outBuffer;
		this.errBuffer = errBuffer;
	}

	@Override
    public String getSessionId()
	{
		return sessionId;
	}

    @SuppressWarnings("nls")
    @Override
    public JShellExecutionResult execute(String code)
	{
		// Clear buffers
		outBuffer.reset();
		errBuffer.reset();

		var result = new JShellExecutionResult();
		result.sessionId = sessionId;
        result.compilationErrors = new java.util.ArrayList<>();
        result.runtimeErrors = new java.util.ArrayList<>();

		// Execute the code
        var events = shell.eval(code);
		if (!events.isEmpty())
		{
            var returnValues = new StringBuilder();
            for (SnippetEvent event : events)
			{
                Snippet.Status status = event.status();

                // Check for compilation errors
                if (status == Snippet.Status.REJECTED)
				{
                    // Get structured diagnostics for compilation errors
                    var diagnostics = shell.diagnostics(event.snippet()).collect(Collectors.toList());
                    if (!diagnostics.isEmpty())
					{
                        for (Diag diag : diagnostics)
                        {
                            var error = new CompilationError();
                            error.isError = diag.isError();
                            error.code = diag.getCode();
                            error.message = diag.getMessage(null);
                            error.position = diag.getPosition();
                            error.startPosition = diag.getStartPosition();
                            error.endPosition = diag.getEndPosition();

                            // Determine the error type based on error code
                            String errorCode = diag.getCode();
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
				}
                else if (status == Snippet.Status.VALID)
				{
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

                    // Get return value
                    if (event.value() != null)
                    {
                        if (returnValues.length() > 0)
                        {
                            returnValues.append("\n");
                        }

                        returnValues.append(event.value());
                    }
				}
			}

            // Set return value
            if (returnValues.length() > 0)
            {
                result.returnValue = returnValues.toString();
			}
		}

		// Capture stdout and stderr
		result.stdOut = outBuffer.toString();
		result.stdErr = errBuffer.toString();

		return result;
	}

	@Override
    public void close()
	{
		shell.close();
	}
}
