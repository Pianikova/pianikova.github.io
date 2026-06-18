/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;
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

public abstract class BaseExecuteMcpTool<TRequest extends BaseExecuteRequest>
    implements IMcpTool
{
    protected final IJson json;
    protected final IMcpToolsCallMessageFactory messageFactory;
    protected final IProcessRunner processRunner;
    protected final IEnvironment environment;
    protected final int DEFAULT_MAX_LINES = McpToolConstants.DEFAULT_MAX_EXECUTION_LINES;

    protected BaseExecuteMcpTool(IEnvironment environment, IJson json, IMcpToolsCallMessageFactory messageFactory,
        IProcessRunner processRunner)
    {
        this.environment = environment;
        this.json = json;
        this.messageFactory = messageFactory;
        this.processRunner = processRunner;
    }

    /**
     * Abstract method to get request type for deserialization.
     * @return The request type
     */
    protected abstract Class<TRequest> getRequestType();

    /**
     * Abstract method to get executable based on request.
     * @param request The request object
     * @return The executable name or path
     */
    protected abstract String getExecutable(TRequest request);

    /**
     * Abstract method to get question example.
     * @return The question example string
     */
    protected abstract String getQuestionExample();

    /**
     * Abstract method to get answer example.
     * @return The answer example string
     */
    protected abstract String getAnswerExample();

    /**
     * Abstract method to validate request.
     * @param request The request object
     * @throws ToolException if validation fails
     */
    protected abstract void validateRequest(TRequest request) throws ToolException;

    /**
     * Abstract method to get tool name.
     * @return The tool name
     */
    protected abstract String getToolName();

    /**
     * Abstract method to get tool description.
     * @return The tool description
     */
    protected abstract String getToolDescription();

    /**
     * Abstract method to add tool-specific properties.
     * @param properties The properties map to add to
     */
    protected abstract void addToolSpecificProperties(java.util.HashMap<String, McpToolCallProperty> properties);

    /**
     * Abstract method to get required parameter names.
     * @return List of required parameter names
     */
    protected abstract java.util.List<String> getRequiredParameters();

    @SuppressWarnings({ "nls" })
    @Override
    public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
    {
        var details = new ToolCallMessageDetails();
        details.autoCall = false;

        var optionalRequest = json.deserialize(call.function.arguments, getRequestType());
        if (optionalRequest.isEmpty())
        {
            throw new ToolException("Cannot deserialize arguments. Use this example: " + getQuestionExample());
        }

        var request = optionalRequest.get();
        validateRequest(request);

        long timeout = request.timeout != null ? request.timeout : 30;
        if (timeout <= 0 || timeout > 300)
        {
            timeout = 30;
        }

        var executable = getExecutable(request);
        var commandLine = buildCommandLine(executable, request.args);

        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = MessageFormat.format(Messages.ExecuteTitleTemplate, commandLine);
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        var futureResult = processRunner.executeProcess(executable, request.working_directory, request.args, timeout,
            TimeUnit.SECONDS, DEFAULT_MAX_LINES);

        return futureResult.thenCompose(optResult -> {
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled during process execution.");
            }

            return optResult.map(response -> {
                var content = json.serialize(response);
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

    @Override
    public McpToolCallSpecification getSpecification()
    {
        return createSpecification();
    }

    @SuppressWarnings("nls")
    protected McpToolCallSpecification createSpecification()
    {
        // @formatter:off
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = getToolName();

        var description = new StringBuilder();
        description.append(getToolDescription());
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- Runs under ").append(environment.getOSName())
                   .append(" ").append(environment.getOSVersion())
                   .append(" (").append(environment.getArch()).append(").");
        description.append("\n\nExample:");
        description.append("\n  Q: ").append(getQuestionExample());
        description.append("\n  A: ").append(getAnswerExample());
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new java.util.HashMap<String, McpToolCallProperty>();

        addToolSpecificProperties(properties);

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
        parameters.required = java.util.Arrays.asList(getRequiredParameters().toArray(new String[0]));

        spec.function.parameters = parameters;
        return spec;
        // @formatter:on
    }

    @SuppressWarnings("nls")
    private String buildResponseMarkdown(ProcessResult response, String commandLine)
    {
        var responseMarkdown = new StringBuilder();
        responseMarkdown.append(MessageFormat.format(Messages.ExecutedTemplate, commandLine));
        responseMarkdown.append("\n\n**").append(Messages.ExecutionDetails).append("**\n\n");

        responseMarkdown.append("__")
            .append(Messages.ExitCode)
            .append(":__ `")
            .append(String.valueOf(response.exitCode))
            .append("`\n");

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

        return responseMarkdown.toString();
    }

    @SuppressWarnings("nls")
    private String getTruncationWarning()
    {
        return "*Warning: " + MessageFormat.format(Messages.OutputTruncatedLines, DEFAULT_MAX_LINES) + "*\n\n";
    }

    @SuppressWarnings("nls")
    private String buildCommandLine(String executable, List<String> args)
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

            commandLine.setLength(commandLine.length() - 1);
        }

        return commandLine.append("`").toString();
    }

    @SuppressWarnings("nls")
    private String escapeArgument(String arg)
    {
        return arg != null && arg.contains(" ") ? "\"" + arg + "\"" : arg;
    }
}
