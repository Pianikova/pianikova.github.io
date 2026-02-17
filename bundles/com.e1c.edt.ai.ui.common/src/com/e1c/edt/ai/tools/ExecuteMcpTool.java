/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class ExecuteMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "Execute"; //$NON-NLS-1$
    private static final int DEFAULT_MAX_LINES = McpToolConstants.DEFAULT_MAX_EXECUTION_LINES;

    private static String QuestionExample =
        "{\"executable\":\"cmd\",\"working_directory\":\"C:\\\\\",\"args\":[\"/c\",\"whoami\"],\"timeout\":3}"; //$NON-NLS-1$

    private static String AnswerExample =
        "{\"exit_code\":0,\"std_out\":\"john_smith\\n\",\"std_err\":\"\"}"; //$NON-NLS-1$

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IProcessRunner processRunner;
    private final IEnvironment environment;

    @Inject
    public ExecuteMcpTool(IEnvironment environment, IJson json, IMcpToolsCallMessageFactory messageFactory,
        IProcessRunner processRunner)
    {
        Preconditions.checkNotNull(environment);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(processRunner);

        this.environment = environment;
        this.json = json;
        this.messageFactory = messageFactory;
        this.processRunner = processRunner;
        spec = createSpecification();
    }

    @Override
    public boolean isExperimental()
    {
        return true;
    }

    @Override
    public McpToolCallSpecification getSpecification()
    {
        return spec;
    }

    @SuppressWarnings({ "nls" })
    @Override
    public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
    {
        var details = new ToolCallMessageDetails();
        details.autoCall = false;

        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
        }

        var request = optionalRequest.get();
        if (request.executable == null || request.executable.isBlank())
        {
            throw new ToolException("`executable` cannot be empty.");
        }

        // Set default timeout if not provided
        long timeout = request.timeout != null ? request.timeout : 30;
        if (timeout <= 0 || timeout > 300)
        {
            timeout = 30; // Default to 30 seconds
        }

        // Build command line for user-friendly display
        var commandLine = buildCommandLine(request.executable, request.args);
        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = MessageFormat.format(Messages.ExecuteTitleTemplate, commandLine);
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        // Execute process with timeout
        var futureResult = processRunner.executeProcess(request.executable,
            request.working_directory, request.args, timeout, TimeUnit.SECONDS, DEFAULT_MAX_LINES);

        return futureResult.thenCompose(optResult -> {
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled during process execution.");
            }

            return optResult.map(response -> {
                var content = json.serialize(response);

                // Build response markdown with output details
                var responseMarkdown = buildResponseMarkdown(response, commandLine);
                details.responseMarkdown = responseMarkdown;

                return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, content, details));
            }).orElseGet(() -> {
                throw new ToolException("Process execution failed - no result.");
            });
        }).exceptionally(error -> {
            Throwable cause = error.getCause();
            throw new ToolException("Process execution failed", cause, ToolErrorType.RETRYABLE);
        });
    }

    /**
     * Builds response markdown with execution details and output
     * @param response Process execution result
     * @param commandLine Command line that was executed
     * @return Formatted markdown string
     */
    @SuppressWarnings("nls")
    private String buildResponseMarkdown(ProcessResult response, String commandLine)
    {
        var responseMarkdown = new StringBuilder();
        responseMarkdown.append(MessageFormat.format(Messages.ExecutedTemplate, commandLine));
        responseMarkdown.append("\n\n<details><summary>").append(Messages.ExecutionDetails).append("</summary>\n\n");

        // Add exit code
        responseMarkdown.append("__")
            .append(Messages.ExitCode)
            .append(":__ `")
            .append(String.valueOf(response.exitCode))
            .append("`\n");

        // Display stdout with truncation warning if needed
        if (response.stdOut != null && !response.stdOut.isEmpty())
        {
            responseMarkdown.append("\n__").append(Messages.StdOutLabel).append(":__\n");
            if (response.stdOutTruncated)
            {
                responseMarkdown.append(getTruncationWarning());
            }

            responseMarkdown.append("```\n");
            responseMarkdown.append(response.stdOut);
            responseMarkdown.append("\n```\n");
        }

        // Display stderr with truncation warning if needed
        if (response.stdErr != null && !response.stdErr.isEmpty())
        {
            responseMarkdown.append("\n__").append(Messages.StdErrLabel).append(":__\n");
            if (response.stdErrTruncated)
            {
                responseMarkdown.append(getTruncationWarning());
            }

            responseMarkdown.append("```\n");
            responseMarkdown.append(response.stdErr);
            responseMarkdown.append("\n```\n");
        }

        responseMarkdown.append("</details>");
        return responseMarkdown.toString();
    }

    /**
     * Returns the truncation warning message
     * @return Truncation warning string
     */
    @SuppressWarnings("nls")
    private String getTruncationWarning()
    {
        return "*Warning: " + MessageFormat.format(Messages.OutputTruncatedLines, DEFAULT_MAX_LINES) + "*\n\n";
    }

    @SuppressWarnings("nls")
    private McpToolCallSpecification createSpecification()
    {
        // @formatter:off
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;

        var description = new StringBuilder();
        description.append("Executes a system process.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Runs under ").append(environment.getOSName())
                   .append(" ").append(environment.getOSVersion())
                   .append(" (").append(environment.getArch()).append(").");
        description.append("\n- Use for OS-level commands, not IDE actions.");
        description.append("\n\nRelated tools:");
        description.append("\n- IDE commands: `" + ExecuteCommandMcpTool.TOOL_NAME + "`.");
        description.append("\n\nExample:");
        description.append("\n  Q: ").append(QuestionExample);
        description.append("\n  A: ").append(AnswerExample);
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        var executableProp = new McpToolCallProperty();
        executableProp.type = "string";
        executableProp.description = "Executable name or path.";
        properties.put("executable", executableProp);

        var workingDirProp = new McpToolCallProperty();
        workingDirProp.type = "string";
        workingDirProp.description = "Working directory (optional)";
        properties.put("working_directory", workingDirProp);

        var argsProp = new McpToolCallProperty();
        argsProp.type = "array";
        argsProp.description = "Command arguments as array of strings";
        properties.put("args", argsProp);

        var timeoutProp = new McpToolCallProperty();
        timeoutProp.type = "integer";
        timeoutProp.description = "Timeout in seconds (1-300, default: 30)";
        properties.put("timeout", timeoutProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("executable");

        spec.function.parameters = parameters;
        return spec;
        // @formatter:on
    }

    private static class Request
    {
        @SerializedName("executable")
        public String executable;

        @SerializedName("working_directory")
        public String working_directory;

        @SerializedName("args")
        public List<String> args;

        @SerializedName("timeout")
        public Long timeout;
    }

    /**
     * Builds user-friendly command line string from executable and arguments.
     */
    @SuppressWarnings("nls")
    private static String buildCommandLine(String executable, List<String> args)
    {
        var commandLine = new StringBuilder();
        commandLine.append("`").append(escapeArgument(executable));

        if (args != null && !args.isEmpty())
        {
            commandLine.append(" ");
            for (String arg : args)
            {
                commandLine.append(escapeArgument(arg)).append(" ");
            }

            // Remove trailing space
            commandLine.setLength(commandLine.length() - 1);
        }

        return commandLine.append("`").toString();
    }

    /**
     * Escapes command line argument by wrapping in quotes if it contains spaces.
     */
    @SuppressWarnings("nls")
    private static String escapeArgument(String arg)
    {
        return arg != null && arg.contains(" ") ? "\"" + arg + "\"" : arg;
    }
}

