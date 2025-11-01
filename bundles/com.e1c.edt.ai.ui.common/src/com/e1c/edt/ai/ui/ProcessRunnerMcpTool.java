/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
    private final IProcessRunner processRunner;
    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;

    @Inject
    public ProcessRunnerMcpTool(IEnvironment environment, IProcessRunner processRunner, IJson json,
        IMcpToolsCallMessageFactory messageFactory)
    {
        Preconditions.checkNotNull(environment);
        Preconditions.checkNotNull(processRunner);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        this.processRunner = processRunner;
        this.json = json;
        this.messageFactory = messageFactory;
        spec = createSpecification(environment);
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
        var optionalCallArgs = json.deserialize(call.function.arguments, Arguments.class);
        if (optionalCallArgs.isEmpty())
        {
            return CompletableFuture
                .completedFuture(messageFactory.createMessage(this, call, "Error: \"Cannot deserialize arguments.\""));
        }

        var callArgs = optionalCallArgs.get();
        var executable = callArgs.get("executable");
        if (executable == null)
        {
            return CompletableFuture
                .completedFuture(
                    messageFactory.createMessage(this, call, "Error: \"Missing required argument 'executable'.\""));
        }

        var workingDirectory = callArgs.get("working_directory");
        var args = json.deserialize(callArgs.get("args"), ArrayList.class).orElse(null);

        @SuppressWarnings("unchecked")
        CompletableFuture<Optional<ProcessResult>> completableFutureResult =
            processRunner.executeProcess(executable, workingDirectory, args);

        return completableFutureResult.thenApply(optResult -> {
            return optResult.map(result -> {
                var content = json.serialize(result);
                return messageFactory.createMessage(this, call, content);
            }).orElseGet(() -> messageFactory.createError(this, call, "Process execution failed - no result."));
        });
    }

    @SuppressWarnings("nls")
    private static McpToolCallSpecification createSpecification(IEnvironment environment)
    {
     // @formatter:off
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = "execute_process";
        spec.function.description =
            "Executes a system process with executable (`executable`), working directory (`working_directory`) and arguments (`args`)."
            + " Returns JSON in `content`: {\"exit_code\":int,\"std_out\":string,\"std_err\":string} e.g. {\"exit_code\":0,\"std_out\":\"Hello\\\\n\",\"std_err\":\"\"}."
            + " Please note that the process executes under " + environment.getOSName()
            + " version " + environment.getOSVersion()
            + " with the " + environment.getArch() + " architecture.";

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        var executableProp = new McpToolCallProperty();
        executableProp.type = "string";
        executableProp.description = "Path to the executable file, use '/' as a directory separator.";
        properties.put("executable", executableProp);

        var workingDirProp = new McpToolCallProperty();
        workingDirProp.type = "string";
        workingDirProp.description = "Working directory for the process, use '/' as a directory separator.";
        properties.put("working_directory", workingDirProp);

        var argsProp = new McpToolCallProperty();
        argsProp.type = "string";
        argsProp.description = "Command-line arguments as a JSON array of strings.";
        properties.put("args", argsProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("executable");
        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }

    private static class Arguments
        extends HashMap<String, String>
    {
    }
}