/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMcpTools;
import com.e1c.edt.ai.McpCallToolsResult;
import com.e1c.edt.ai.assistent.SkillErrorCode;
import com.e1c.edt.ai.assistent.SkillExecutionException;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunctionCall;
import com.e1c.edt.ai.assistent.model.McpToolCalls;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class McpToolInvoker
    implements IMcpToolInvoker
{
    private final IMcpTools mcpTools;
    private final IJson json;

    @Inject
    public McpToolInvoker(IMcpTools mcpTools, IJson json)
    {
        Preconditions.checkNotNull(mcpTools);
        Preconditions.checkNotNull(json);
        this.mcpTools = mcpTools;
        this.json = json;
    }

    @Override
    public CompletableFuture<String> invokeAsync(ToolRequestSpecification request, ICancellationToken token)
    {
        var toolCall = new McpToolCall();

        toolCall.type = "function"; //$NON-NLS-1$
        toolCall.id = createCallId(request.getName());

        var function = new McpToolCallFunctionCall();
        function.name = request.getName();
        function.arguments = json.serialize(request.getArguments());

        toolCall.function = function;

        var toolCalls = new McpToolCalls();
        toolCalls.add(toolCall);

        return mcpTools.callTools(toolCalls, token).thenApply(result -> extractResult(request.getName(), result));
    }

    @SuppressWarnings("nls")
    private String extractResult(String name, McpCallToolsResult result) throws SkillExecutionException
    {
        if (result == null || result.messages == null || result.messages.isEmpty())
        {
            throw new SkillExecutionException(SkillErrorCode.TOOL_EXECUTION_ERROR,
                "No result returned from tool " + name);
        }

        var message = result.messages.get(0);

        if (message.details != null && message.details.responseMarkdown != null)
        {
            return message.details.responseMarkdown;
        }

        if (message.content != null)
        {
            return message.content;
        }

        throw new SkillExecutionException(SkillErrorCode.TOOL_EXECUTION_ERROR,
            "Tool call result does not contain content: " + message);
    }

    @SuppressWarnings("nls")
    private String createCallId(String name)
    {
        return "skill_tool_" + name + "_" + System.currentTimeMillis();
    }

}
