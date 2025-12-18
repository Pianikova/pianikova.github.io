/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
import com.e1c.edt.ai.assistent.model.ProcessResult;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ProcessRunnerMcpTool
    implements IMcpTool
{
    private static String QuestionExample =
        "{\"executable\":\"cmd\",\"working_directory\":\"C:\\\\\\\\\",\"args\":[\"/c\",\"whoami\"],\"timeout\":3000}"; //$NON-NLS-1$

    private static String AnswerExample =
        "{\"exit_code\":0,\"std_out\":\"john_smith\\n\",\"std_err\":\"\"}"; //$NON-NLS-1$

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IProcessRunner processRunner;

    @Inject
    public ProcessRunnerMcpTool(IEnvironment environment, IJson json, IMcpToolsCallMessageFactory messageFactory,
        IProcessRunner processRunner)
    {
        Preconditions.checkNotNull(environment);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(processRunner);
        this.json = json;
        this.messageFactory = messageFactory;
        this.processRunner = processRunner;
        spec = createSpecification(environment);
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
        var optionalCallArgs = json.deserialize(call.function.arguments, CallArguments.class);
        if (optionalCallArgs.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call,
                    "Cannot deserialize arguments. Use this example: " + QuestionExample));
        }

        var callArgs = optionalCallArgs.get();
        if (callArgs.executable == null || callArgs.executable.isBlank())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createError(this, call, "'executable' cannot be empty."));
        }

        CompletableFuture<Optional<ProcessResult>> completableFutureResult =
            processRunner.executeProcess(callArgs.executable, callArgs.working_directory, callArgs.args,
                callArgs.timeout, TimeUnit.SECONDS);

        return completableFutureResult.thenApply(optResult -> {
            return optResult.map(result -> {
                result.stdOut = shrink(result.stdOut);
                result.stdErr = shrink(result.stdErr);
                var content = json.serialize(result);
                return messageFactory.createMessage(this, call, content);
            })
                .orElseGet(
                    () -> messageFactory.createError(this, call, "Process execution failed - no result."));
        });
    }

    private static String shrink(String text)
    {
        if (text == null || text.length() < 0x3fff)
        {
            return text;
        }

        return text.substring(0, 0x3fff) + "\n...\nError: \"The answer is too big.\""; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification(IEnvironment environment)
    {
     // @formatter:off
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = "execute_process";

        var description = new StringBuilder();

        description.append("Executes a system process.");

        description.append("\nIMPORTANT: the process executes under ");
        description.append(environment.getOSName());
        description.append(" version ");
        description.append(environment.getOSVersion());
        description.append(" with the ");
        description.append(environment.getArch());
        description.append(" architecture.");

        description.append("\nIMPORTANT: use only non-interactive mode when executing a process.");
        description.append("\nNOTE: add a description of what will be done when using this tool.");

        var tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir != null && !tempDir.isBlank())
        {
            description.append("\nNOTE: use scripts to minimize the number of tool calls.");
            description.append("\nNOTE: to create scripts use the '");
            description.append(tempDir);
            description.append("' directory.");
        }

        description.append("\nFor exapmple:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);

        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        var executableProp = new McpToolCallProperty();
        executableProp.type = "string";
        executableProp.description = "Path to the executable file.";
        properties.put("executable", executableProp);

        var workingDirProp = new McpToolCallProperty();
        workingDirProp.type = "string";
        workingDirProp.description = "Working directory for the process.";
        properties.put("working_directory", workingDirProp);

        var argsProp = new McpToolCallProperty();
        argsProp.type = "object";
        argsProp.description = "Сommand-line arguments as a JSON array of strings.";
        properties.put("args", argsProp);

        var timeoutProp = new McpToolCallProperty();
        argsProp.type = "integer";
        argsProp.description = "The timeout for a process to execute, in seconds.";
        properties.put("timeout", timeoutProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("executable");
        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }

    private static class CallArguments
    {
        public String executable;

        public String working_directory;

        public ArrayList<String> args;

        public Long timeout;
    }
}