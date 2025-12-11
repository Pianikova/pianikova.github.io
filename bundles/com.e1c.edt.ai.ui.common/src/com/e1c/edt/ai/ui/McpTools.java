/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpTools;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.McpCallToolsResult;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.McpToolCalls;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class McpTools
    implements IMcpTools
{
    private final Map<String, IMcpTool> tools = new HashMap<>();
    private final List<McpToolCallSpecification> specs = new ArrayList<>();
    private final IMcpToolsCallMessageFactory messageFactory;

    @Inject
    public McpTools(Set<IMcpTool> tools, IMcpToolsCallMessageFactory messageFactory)
    {
        Preconditions.checkNotNull(tools);
        Preconditions.checkNotNull(messageFactory);
        this.messageFactory = messageFactory;
        for (IMcpTool tool : tools)
        {
            var spec = tool.getSpecification();
            if (spec == null)
            {
                continue;
            }

            var function = spec.function;
            if (function == null)
            {
                continue;
            }

            var name = function.name;
            if (name == null)
            {
                continue;
            }

            if (this.tools.putIfAbsent(name.toLowerCase(), tool) == null)
            {
                specs.add(spec);
            }
        }
    }

    @Override
    public List<McpToolCallSpecification> getSpecifications()
    {
        return Collections.unmodifiableList(specs);
    }

    @Override
    public CompletableFuture<McpCallToolsResult> callTools(McpToolCalls calls, ICancellationToken cancellationToken)
    {
        var futures = new ArrayList<CompletableFuture<ToolCallMessage>>();
        var unknownCalls = new McpToolCalls();
        for (var call : calls)
        {
            var toolName = call.function.name.toLowerCase();
            var tool = tools.get(toolName);
            if (tool == null)
            {
                unknownCalls.add(call);
                continue;
            }

            var callFuture = tool.call(call, cancellationToken)
                .exceptionally(ex -> messageFactory.createError(null, call, ex.getMessage()));
            futures.add(callFuture);
        }

        if (futures.isEmpty())
        {
            var result = new McpCallToolsResult();
            result.messages = new ArrayList<>();
            result.unknownCalls = unknownCalls;
            return CompletableFuture.completedFuture(result);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                var result = new McpCallToolsResult();
                result.messages = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
                result.unknownCalls = unknownCalls;
                return result;
            });
    }
}