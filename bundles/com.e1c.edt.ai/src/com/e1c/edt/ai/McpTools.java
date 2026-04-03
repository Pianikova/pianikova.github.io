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
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.McpToolCalls;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

public class McpTools
    implements IMcpTools
{
    private static final int MAX_CONTENT_LINES = 5000;
    private static final Pattern LINE_SEPARATOR = Pattern.compile("\\r\\n|\\r|\\n"); //$NON-NLS-1$

    private final ILog log;
    private final ISettings settings;
    private final Map<String, IMcpTool> tools = new HashMap<>();
    private final List<McpToolCallSpecification> specs = new ArrayList<>();
    private final IMcpToolsCallMessageFactory messageFactory;
    private final IJson json;
    private final Cache<String, List<McpToolCallSpecification>> specsCache =
        CacheBuilder.newBuilder().maximumSize(1).expireAfterWrite(1, TimeUnit.HOURS).build();

    @Inject
    public McpTools(ILog log, ISettings settings, Set<IMcpTool> tools, IMcpToolsCallMessageFactory messageFactory,
        IJson json)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(tools);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(json);

        this.log = log;
        this.settings = settings;
        this.messageFactory = messageFactory;
        this.json = json;

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

    @SuppressWarnings("nls")
    @Override
    public CompletableFuture<List<McpToolCallSpecification>> getSpecifications()
    {
        var cachedSpecs = specsCache.getIfPresent("specs"); //$NON-NLS-1$
        if (cachedSpecs != null)
        {
            return CompletableFuture.completedFuture(cachedSpecs);
        }

        var availabilityFutures = new ArrayList<CompletableFuture<AvailableTool>>();
        for (var entry : tools.entrySet())
        {
            var tool = entry.getValue();

            availabilityFutures.add(tool.getIsAvailable().handle((available, ex) -> {
                if (ex != null)
                {
                    try
                    {
                        var spec = tool.getSpecification();
                        var toolName = spec != null && spec.function != null ? spec.function.name : "unknown";
                        log.warning("Tool '" + toolName + "' availability check failed", () -> ex.toString());
                    }
                    catch (Exception e)
                    {
                        log.warning("Tool availability check failed", () -> ex.toString()); //$NON-NLS-1$
                    }

                    return false;
                }

                return available;
            }).thenApply(available -> {
                var availableTool = new AvailableTool();
                availableTool.tool = tool;
                availableTool.isAvailable = available;
                return availableTool;
            }));
        }

        CompletableFuture<Void> allOfFuture =
            CompletableFuture.allOf(availabilityFutures.toArray(new CompletableFuture[0]));
        return allOfFuture.thenApply(v -> {
            var resultSpecs = new ArrayList<McpToolCallSpecification>();

            for (var future : availabilityFutures)
            {
                var availableTool = future.join();
                if (!availableTool.isAvailable)
                {
                    continue;
                }

                var spec = availableTool.tool.getSpecification();
                if (spec != null && spec.function != null && spec.function.name != null)
                {
                    resultSpecs.add(spec);
                }
            }

            specsCache.put("specs", resultSpecs); //$NON-NLS-1$
            return resultSpecs;
        });
    }

    private static class AvailableTool
    {
        IMcpTool tool;
        boolean isAvailable;
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
                @SuppressWarnings("nls")
                var callFuture =
                    tool.call(call, cancellationToken)
                        .exceptionally(error -> {
                            log.warning("AI Tool failed", () -> {
                                var message = new StringBuilder();
                                message.append(error.toString());
                                message.append("\n\nCall:\n\n");
                                message.append(json.serialize(call));
                                return message.toString();
                            });
                            return createErrorMessage(tool, call, toolName, error);
                        });
                futures.add(callFuture);
            }
            catch (Exception ex)
            {
                var message = createErrorMessage(tool, call, toolName, ex);
                futures.add(CompletableFuture.completedFuture(message));
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

    private ToolCallMessage createErrorMessage(IMcpTool tool, McpToolCall call, String toolName, Throwable error)
    {
        var details = new ToolCallMessageDetails();
        var message = error.getMessage();

        // Build detailed message in trace mode
        if (isLogLevel(Verbosity.TRACE))
        {
            var traceMessage = new StringBuilder();
            traceMessage.append(message);
            traceMessage.append("\n\n"); //$NON-NLS-1$

            // Add cause if present
            var cause = error.getCause();
            if (cause != null)
            {
                traceMessage.append("Cause: "); //$NON-NLS-1$
                traceMessage.append(cause.getMessage());
                traceMessage.append("\n\n"); //$NON-NLS-1$
            }

            // Add stack trace
            traceMessage.append("Stack trace:\n"); //$NON-NLS-1$
            for (var stackElement : error.getStackTrace())
            {
                traceMessage.append("\tat "); //$NON-NLS-1$
                traceMessage.append(stackElement.toString());
                traceMessage.append("\n"); //$NON-NLS-1$
            }

            message = traceMessage.toString();
        }

        if (error instanceof ToolException)
        {
            var toolError = (ToolException)error;
            switch (toolError.getErrorType())
            {
            case USER_VISIBLE:
                // User-visible errors should be shown to user
                var markdown = new StringBuilder();
                markdown.append(message);
                if (call.callKind == ToolCallKind.RENDER)
                {
                    details.requestMarkdown = markdown.toString();
                }
                else
                {
                    details.responseMarkdown = markdown.toString();
                }

                details.autoCall = true;
                return messageFactory.createMessage(tool, call, message, details);
            }
        }

        // Retryable errors are handled silently by LLM, log as warning for debugging
        details.autoCall = true;
        details.hideAfter = true;
        details.responseMarkdown = Messages.McpTools_RetryableError;
        return messageFactory.createRawMessage(tool, call, message, details);
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

    private boolean isLogLevel(Verbosity verbosity)
    {
        return settings.getVerbosity().getLevel() >= verbosity.getLevel();
    }
}