/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;

public class ProcessRunnerMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "execute_process"; //$NON-NLS-1$
    private static final int MAX_OUTPUT_LENGTH = 16384; // 16KB

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
    public ProcessRunnerMcpTool(IEnvironment environment, IJson json, IMcpToolsCallMessageFactory messageFactory,
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
        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }

        var request = optionalRequest.get();
        if (request.executable == null || request.executable.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "'executable' cannot be empty."));
        }

        // Set default timeout if not provided
        long timeout = request.timeout != null ? request.timeout : 30;
        if (timeout <= 0 || timeout > 300)
        {
            timeout = 30; // Default to 30 seconds
        }

        // Execute process with timeout
        var futureResult = processRunner.executeProcess(request.executable,
            request.working_directory, request.args, timeout, TimeUnit.SECONDS);

        // Handle cancellation
        var resultFuture = futureResult.thenApply(optResult -> {
            if (cancellationToken.isCanceled())
            {
                return messageFactory.createError(this, call, "Operation was cancelled during process execution.");
            }

            return optResult.map(response -> {
                response.stdOut = truncateOutput(response.stdOut);
                response.stdErr = truncateOutput(response.stdErr);
                var content = json.serialize(response);
                return messageFactory.createMessage(this, call, content);
            })
                .orElseGet(() -> messageFactory.createError(this, call, "Process execution failed - no result."));
        });

        // Add cancellation handling
        CancellationTokenSource.attach(cancellationToken, () -> {
            futureResult.cancel(true);
        });

        return resultFuture;
    }

    @SuppressWarnings("nls")
    private String truncateOutput(String text)
    {
        if (text == null)
        {
            return null;
        }
        if (text.length() <= MAX_OUTPUT_LENGTH)
        {
            return text;
        }
        return text.substring(0, MAX_OUTPUT_LENGTH) + "\n...[OUTPUT TRUNCATED TO " + MAX_OUTPUT_LENGTH + " CHARACTERS]";
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
        description.append("Executes a system process with limited capabilities.");
        description.append("\nIMPORTANT: Runs under ").append(environment.getOSName())
                   .append(" ").append(environment.getOSVersion())
                   .append(" (").append(environment.getArch()).append(")");
        description.append("\nSECURITY: Only basic commands allowed (cmd, powershell, bash, .bat, .sh)");
        description.append("\nWARNING: Processes run with limited permissions and time (max 5 minutes)");
        description.append("\nNOTE: Use for simple system commands only");
        description.append("\n\nExample:");
        description.append("\n  Q: ").append(QuestionExample);
        description.append("\n  A: ").append(AnswerExample);
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        var executableProp = new McpToolCallProperty();
        executableProp.type = "string";
        executableProp.description = "Executable name or path (allowed: cmd, powershell, bash, .bat, .sh)";
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
}