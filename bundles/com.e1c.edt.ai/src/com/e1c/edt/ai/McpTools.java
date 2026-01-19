/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.McpToolCalls;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class McpTools
    implements IMcpTools
{
    private static final int MAX_CONTENT_LINES = 5000;
    private static final Pattern LINE_SEPARATOR = Pattern.compile("\\r\\n|\\r|\\n"); //$NON-NLS-1$

    private final ILog log;
    private final Map<String, IMcpTool> tools = new HashMap<>();
    private final List<McpToolCallSpecification> specs = new ArrayList<>();
    private final IMcpToolsCallMessageFactory messageFactory;

    @Inject
    public McpTools(ILog log, ISettings settings, Set<IMcpTool> tools, IMcpToolsCallMessageFactory messageFactory)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(tools);
        Preconditions.checkNotNull(messageFactory);
        this.log = log;
        this.messageFactory = messageFactory;
        if (!settings.isExperimental())
        {
            tools = tools.stream().filter(i -> !i.isExperimental()).collect(Collectors.toSet());
        }

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
        return specs;
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

            try
            {
                var callFuture = tool.call(call, cancellationToken).exceptionally(ex -> {
                    var message = createErrorMessage(toolName, ex);
                    log.logError(message);
                    return messageFactory.createError(null, call, ex.getMessage());
                });

                futures.add(callFuture);
            }
            catch (Exception ex)
            {
                var message = createErrorMessage(toolName, ex);
                log.logError(message);
                futures.add(CompletableFuture.completedFuture(messageFactory.createError(tool, call, message)));
            }
        }

        if (futures.isEmpty())
        {
            var result = new McpCallToolsResult();
            result.messages = new ArrayList<>();
            result.unknownCalls = unknownCalls;
            return CompletableFuture.completedFuture(result);
        }

        @SuppressWarnings("nls")
        var futureResult = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                var result = new McpCallToolsResult();
                var messages = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

                // Validate content size for each message
                for (var message : messages)
                {
                    if (message.content != null && exceedsMaxContentLines(message.content))
                    {
                        // Replace content with error message if it exceeds limit
                        message.content = "Error: Content exceeds maximum allowed lines (" + MAX_CONTENT_LINES + ").";
                        log.logError("Tool response content exceeds maximum allowed lines for tool call: "
                            + (message.call != null ? message.call.function.name : "unknown"));
                    }
                }

                result.messages = messages;
                result.unknownCalls = unknownCalls;
                return result;
            });

        // Add cancellation handling
        CancellationTokenSource.attach(cancellationToken, () -> {
            futureResult.cancel(true);
        });

        return futureResult;
    }

    @SuppressWarnings("nls")
    private String createErrorMessage(String toolName, Throwable ex)
    {
        var cause = ex instanceof CompletionException ? ex.getCause() : ex;
        var message = "Failed to call tool \"" + toolName + "\". " + cause.getMessage();
        return message;
    }

    private boolean exceedsMaxContentLines(String content)
    {
        if (content == null || content.isEmpty())
        {
            return false;
        }

        var lines = LINE_SEPARATOR.split(content);
        return lines.length > MAX_CONTENT_LINES;
    }
}