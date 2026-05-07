/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
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

public class JShellReflectionMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "JShellReflection"; //$NON-NLS-1$

    private static final String QUESTION_EXAMPLE =
        "{\"repl_session_id\":\"550e8400-e29b-41d4-a716-446655440000\",\"queries\":[\"com._1c.g5.v8.dt.metadata.mdclass\",\"TypeDescriptionBuilder.set*\"]}"; //$NON-NLS-1$

    private final IJson json;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IJShellSessionManager sessions;
    private final IJShellReflectionService reflectionService;
    private final McpToolCallSpecification specification;

    @Inject
    public JShellReflectionMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        IJShellSessionManager sessions, IJShellReflectionService reflectionService)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(sessions);
        Preconditions.checkNotNull(reflectionService);

        this.json = json;
        this.messageFactory = messageFactory;
        this.sessions = sessions;
        this.reflectionService = reflectionService;
        this.specification = createSpecification();
    }

    @Override
    public boolean isExperimental()
    {
        return true;
    }

    @Override
    public McpToolCallSpecification getSpecification()
    {
        return specification;
    }

    @Override
    public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
    {
        var details = new ToolCallMessageDetails();
        details.autoCall = false;

        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            throw new ToolException("Cannot deserialize arguments. Use this example: " + QUESTION_EXAMPLE); //$NON-NLS-1$
        }

        var request = optionalRequest.get();
        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = Messages.JShellReflectionRequest;
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        return CompletableFuture.supplyAsync(() -> execute(request, call, details, cancellationToken));
    }

    private ToolCallMessage execute(Request request, McpToolCall call, ToolCallMessageDetails details,
        ICancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled())
        {
            throw new ToolException("Operation was cancelled before execution.", null, ToolErrorType.RETRYABLE); //$NON-NLS-1$
        }

        var session = sessions.getSession(request.sessionId);
        if (session == null)
        {
            throw new ToolException(
                "Session not found. Use " + JShellSessionMcpTool.TOOL_NAME + " tool to create a session first.", //$NON-NLS-1$ //$NON-NLS-2$
                null,
                ToolErrorType.RETRYABLE);
        }

        var result = reflectionService.search(session, request.queries);
        if (result == null || result.isEmpty())
        {
            throw new ToolException("Can't analyze API.", null, ToolErrorType.RETRYABLE); //$NON-NLS-1$
        }

        details.responseMarkdown = Messages.JShellReflectionResponse;
        return messageFactory.createMessage(this, call, json.serialize(result), details);
    }

    @SuppressWarnings("nls")
    private McpToolCallSpecification createSpecification()
    {
        var spec = new McpToolCallSpecification();
        spec.type = "function";
        spec.function = new McpToolCallFunction();
        spec.function.name = TOOL_NAME;
        spec.function.description = buildDescription();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";
        var properties = new HashMap<String, McpToolCallProperty>();

        var queriesProperty = new McpToolCallProperty();
        queriesProperty.type = "array";
        queriesProperty.description = "Required array of API search strings. Supports `*` wildcards. Examples: "
            + "`com._1c.g5.v8.dt.metadata.mdclass`, `com._1c.g5.v8.dt.metadata.mdclass.*HierarchyType`, "
            + "`TypeDescriptionBuilder.setNumberQualifiers`, `TypeDescriptionBuilder.set*`.";
        properties.put("queries", queriesProperty);

        var sessionIdProperty = new McpToolCallProperty();
        sessionIdProperty.type = "string";
        sessionIdProperty.description = "Session ID (UUID string) from " + JShellSessionMcpTool.TOOL_NAME + " tool.";
        properties.put("repl_session_id", sessionIdProperty);

        parameters.properties = properties;
        parameters.required = new ArrayList<>();
        parameters.required.add("queries");
        parameters.required.add("repl_session_id");
        spec.function.parameters = parameters;
        return spec;
    }

    @SuppressWarnings("nls")
    private String buildDescription()
    {
        var description = new StringBuilder();
        description.append("Analyzes Java API available in the current ")
            .append(JShellSessionMcpTool.TOOL_NAME)
            .append(" session. Use before ")
            .append(JShellMcpTool.TOOL_NAME)
            .append(" when unsure about packages, types, enum constants, methods, fields, constructors, or signatures.");
        description.append("\n\nUse this tool to determine the exact Java API. Do not rely on memory or guesswork for ")
            .append("method names, overloads, enum constants, package names, or type names.");
        description.append("\n\nAccepts multiple search strings in one call and returns results in the same order.");
        description.append("\nSupports `*` wildcards for type and member lookup.");
        description.append("\nOne query can return multiple search results; each result contains a flat `items` list.");
        description.append("\nIf a query is not found, use returned `suggestions` for the next ")
            .append(TOOL_NAME)
            .append(" call instead of inventing API names.");
        description.append("\nUse after JShell errors like `cannot find symbol`, `method not found`, or `package does not exist` instead of guessing APIs.");
        description.append("\n\nExample: `").append(QUESTION_EXAMPLE).append("`");
        return description.toString();
    }

    private static class Request
    {
        @SerializedName("queries")
        public List<String> queries;

        @SerializedName("repl_session_id")
        public String sessionId;
    }
}
