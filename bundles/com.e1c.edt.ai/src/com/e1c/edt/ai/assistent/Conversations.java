/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IConversationProgressListener;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMcpTools;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.McpCallToolsResult;
import com.e1c.edt.ai.Observables;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.model.AssistantMessageContent;
import com.e1c.edt.ai.assistent.model.AssistantMessageContentDelta;
import com.e1c.edt.ai.assistent.model.ChoiceDeltaToolCall;
import com.e1c.edt.ai.assistent.model.ConversationAskRequest;
import com.e1c.edt.ai.assistent.model.ConversationAskResponse;
import com.e1c.edt.ai.assistent.model.ConversationRequest;
import com.e1c.edt.ai.assistent.model.ConversationResponse;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunctionCall;
import com.e1c.edt.ai.assistent.model.McpToolCalls;
import com.e1c.edt.ai.assistent.model.Session;
import com.e1c.edt.ai.assistent.model.ToolMessageContent;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;

/**
 * Класс для управления диалогами с AI-ассистентом.
 * <p>
 * Предоставляет функционал для создания диалогов и отправки сообщений с поддержкой инструментов MCP.
 * Работает асинхронно и поддерживает отмену операций.
 * </p>
 *
 * @author Bogdan Sushkov
 */
public class Conversations implements IConversations
{
    /**
     * Tool-round nesting is unbounded: a turnkey task (build a whole 1C configuration with entities,
     * forms, templates and code modules) needs an open-ended number of rounds, and truncating it
     * mid-run leaves the project half-built. Termination comes from the model itself (no more tool
     * calls) or from cancellation, not from a round ceiling.
     */
    private static final int MAX_TOOL_ROUNDS = Integer.MAX_VALUE;

    private final IHttpLog log;
    private final ISettings settings;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clientBuilder;
    private final IJson json;
    private final ISessionService sessionService;
    private final IMcpTools mcpTools;
    private final ILog logDebug;

    @Inject
    public Conversations(IHttpLog log, ISettings settings, IRequestBuilder requestBuilder,
        IHttpClientBuilder clientBuilder, IJson json, ISessionService sessionService, IMcpTools mcpTools, ILog logDebug)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(mcpTools);
        Preconditions.checkNotNull(logDebug);
        this.mcpTools = mcpTools;
        this.log = log;
        this.settings = settings;
        this.requestBuilder = requestBuilder;
        this.clientBuilder = clientBuilder;
        this.json = json;
        this.sessionService = sessionService;
        this.logDebug = logDebug;
    }

    /**
     * Создает новый диалог с AI-ассистентом асинхронно.
     *
     * @param projectId идентификатор проекта
     * @param request параметры создания диалога
     * @param cancellationToken токен для отмены операции
     * @return {@link CompletableFuture} с {@link Optional}, содержащим {@link ConversationResponse}
     *         или {@link Optional#empty()}, если диалог не удалось создать
     */
    @Override
    public CompletableFuture<Optional<ConversationResponse>> createConversationAsync(IProject project,
        ConversationRequest request, ICancellationToken cancellationToken)
    {
        return sessionService.getSessionAsync(project).<Optional<ConversationResponse>> thenApplyAsync(session -> {
            if (session.isEmpty())
            {
                return Optional.empty();
            }

            try
            {
                return createConversation(session.get(), request, cancellationToken);
            }
            catch (Exception error)
            {
                log.error(error, cancellationToken.toString());
            }

            return Optional.empty();
        });
    }

    /**
     * Создает источник ответов для отправки сообщений в диалог с поддержкой инструментов MCP.
     * <p>
     * Обрабатывает вызовы инструментов автоматически, выполняя итерации до 10 раундов.
     * Поддерживает отмену операции через {@link ICancellationToken}.
     * </p>
     *
     * @param projectId идентификатор проекта
     * @param conversationId идентификатор диалога
     * @param request параметры запроса к ассистенту
     * @param cancellationToken токен для отмены операции
     * @return {@link IObservable}, который публикует ответы ассистента {@link ConversationAskResponse}
     */
    @Override
    public IObservable<ConversationAskResponse> createAskSource(IProject project, String conversationId,
        ConversationAskRequest request, ICancellationToken cancellationToken,
        IConversationProgressListener progressListener)
    {
        return Observables.create(observer -> {
            sessionService.getSessionAsync(project).whenComplete((session, error) -> {
                  if (error != null)
                  {
                      observer.onError(error);
                      return;
                  }

                  if (session.isEmpty()) {
                      observer.onCompleted();
                      return;
                  }
                askWithTools(session.get(), conversationId, request, observer, cancellationToken, 0,
                    progressListener);
            });
            return Closeables.Empty;
        });
    }

    private void askWithTools(Session session, String conversationId, ConversationAskRequest request,
        IObserver<ConversationAskResponse> observer, ICancellationToken cancellationToken, int depth,
        IConversationProgressListener progressListener)
    {
        int maxToolRounds = request.maxToolRounds > 0 ? request.maxToolRounds : MAX_TOOL_ROUNDS;
        if (depth > maxToolRounds)
        {
            observer.onError(new IllegalStateException("Too many tool rounds")); //$NON-NLS-1$
            return;
        }
        askSingleRound(session, conversationId, request, observer, cancellationToken).whenComplete((roundResult, error) -> {
            if (error != null)
            {
                if (!isCancellationException(error)) {
                    log.error(error, cancellationToken.toString());
                    observer.onError(error);
                }
                else
                {
                    observer.onCompleted();
                }
                return;
            }
            if (cancellationToken.isCanceled())
            {
                observer.onCompleted();
                return;
            }
            ConversationAskResponse lastResponse = roundResult != null ? roundResult.lastResponse : null;
            if (lastResponse == null)
            {
                observer.onCompleted();
                return;
            }

            ArrayList<McpToolCall> toolCalls = extractToolCalls(lastResponse, conversationId);
            if (toolCalls.isEmpty())
            {
                observer.onCompleted();
                return;
            }
            handleToolCalls(session, conversationId, lastResponse, toolCalls, observer, cancellationToken, depth + 1,
                maxToolRounds, progressListener);
        });

    }

    private void handleToolCalls(Session session, String conversationId, ConversationAskResponse lastResponse,
        ArrayList<McpToolCall> toolCalls, IObserver<ConversationAskResponse> observer,
        ICancellationToken cancellationToken, int depth, int maxToolRounds,
        IConversationProgressListener progressListener)
    {
        if (cancellationToken.isCanceled()) {
            observer.onCompleted();
            return;
        }

        McpToolCalls calls = new McpToolCalls();
        calls.addAll(toolCalls);

        List<String> toolNames =
            toolCalls.stream().map(call -> call.function.name).collect(Collectors.toList());
        reportToolCallStart(progressListener, toolNames);

        CompletableFuture<McpCallToolsResult> future;
        try {

            future = mcpTools.callTools(calls, cancellationToken);
        }
        catch (Exception e)
        {
            log.error(e, cancellationToken.toString());
            observer.onError(e);
            return;
        }

        future.whenComplete((toolResult, error) -> {
            reportToolCallEnd(progressListener, toolNames);
            if (cancellationToken.isCanceled())
            {
                observer.onCompleted();
                return;
            }
            if (error != null)
            {
                log.error(error, cancellationToken.toString());
                observer.onError(error);
                return;
            }
            try {
                ConversationAskRequest toolRequest = createToolRequest(lastResponse.uuid, toolResult);
                toolRequest.maxToolRounds = maxToolRounds;
                askWithTools(session, conversationId, toolRequest, observer, cancellationToken, depth,
                    progressListener);
            }
            catch (Exception e)
            {
                log.error(e, cancellationToken.toString());
                observer.onError(e);
            }
        });
    }

    /**
     * Reports a tool-call batch starting, shielding execution from a misbehaving listener: a listener
     * that throws must not abort the tool calls it is only observing.
     */
    private void reportToolCallStart(IConversationProgressListener progressListener, List<String> toolNames)
    {
        if (progressListener == null)
        {
            return;
        }

        try
        {
            progressListener.onToolCallStart(toolNames);
        }
        catch (RuntimeException error)
        {
            log.error(error, toolNames.toString());
        }
    }

    /** @see #reportToolCallStart */
    private void reportToolCallEnd(IConversationProgressListener progressListener, List<String> toolNames)
    {
        if (progressListener == null)
        {
            return;
        }

        try
        {
            progressListener.onToolCallEnd(toolNames);
        }
        catch (RuntimeException error)
        {
            log.error(error, toolNames.toString());
        }
    }

    private ConversationAskRequest createToolRequest(String parentMessageUuid, McpCallToolsResult toolResult)
    {
        ConversationAskRequest request = new ConversationAskRequest();
        request.parentUuid = parentMessageUuid;
        request.role = "tool"; //$NON-NLS-1$

        ArrayList<ToolMessageContent> items = new ArrayList<>();

        if (toolResult != null && toolResult.messages != null)
        {
            for (ToolCallMessage message : toolResult.messages)
            {
                ToolMessageContent item = new ToolMessageContent();
                item.toolCallId = message.tool_call_id;
                item.content = Optional.ofNullable(message.content);
                item.status = "ok"; //$NON-NLS-1$
                items.add(item);
            }
        }

        if (toolResult != null && toolResult.unknownCalls != null)
        {
            for (McpToolCall call : toolResult.unknownCalls)
            {
                ToolMessageContent item = new ToolMessageContent();
                item.toolCallId = call != null ? call.id : null;
                // A tool the client cannot execute is treated as a server tool: reply with
                // status "accepted" and an explicit null content, exactly like the TypeScript
                // chat client. For server tools (TodoWrite, Task, server MCP) the gateway then
                // executes the tool itself and continues the generation; for tools unknown to
                // the server too it produces the canonical "unknown tool" error result. Any
                // other status on a server tool is rejected by the server with HTTP 422.
                item.content = Optional.empty();
                item.status = "accepted"; //$NON-NLS-1$
                items.add(item);
            }
        }

        request.content = jsonToElement(items);
        return request;
    }

    private JsonElement jsonToElement(Object value)
    {
        if (value == null)
        {
            return null;
        }
        return json.deserialize(json.serialize(value), JsonElement.class).orElse(null);
    }

    private ArrayList<McpToolCall> extractToolCalls(ConversationAskResponse response, String conversationId)
    {
        ArrayList<McpToolCall> result = new ArrayList<>();
        if (response == null || response.content == null)
        {
            return result;
        }

        if (response.content.toolCalls != null)
        {
            for (McpToolCall call : response.content.toolCalls)
            {
                if (call == null)
                {
                    continue;
                }

                McpToolCall newCall = new McpToolCall();
                newCall.id = call.id;
                newCall.function = call.function;
                newCall.sourceChatId = conversationId;
                newCall.sourceMessageId = response.uuid;
                result.add(newCall);
            }
        }

        // Fallback for models that do not return structured tool_calls (e.g. MiniMax): they emit the
        // call inside the assistant text using Anthropic-style
        // <invoke name="Tool"><parameter name="p">value</parameter></invoke> blocks. Parse those out of
        // the content so the tools still execute instead of leaking as plain text.
        if (result.isEmpty() && response.content.content != null)
        {
            for (McpToolCall call : parseTextToolCalls(response.content.content))
            {
                call.sourceChatId = conversationId;
                call.sourceMessageId = response.uuid;
                result.add(call);
            }
        }

        return result;
    }

    private static final Pattern INVOKE_PATTERN =
        Pattern.compile("<invoke\\s+name=\"([^\"]+)\"\\s*>(.*?)</invoke>", Pattern.DOTALL); //$NON-NLS-1$
    private static final Pattern PARAMETER_PATTERN =
        Pattern.compile("<parameter\\s+name=\"([^\"]+)\"\\s*>(.*?)</parameter>", Pattern.DOTALL); //$NON-NLS-1$
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?\\d+"); //$NON-NLS-1$
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("-?\\d+\\.\\d+"); //$NON-NLS-1$

    private ArrayList<McpToolCall> parseTextToolCalls(String text)
    {
        ArrayList<McpToolCall> calls = new ArrayList<>();
        if (text == null || !text.contains("<invoke")) //$NON-NLS-1$
        {
            return calls;
        }
        Matcher invoke = INVOKE_PATTERN.matcher(text);
        while (invoke.find())
        {
            String name = invoke.group(1).trim();
            if (name.isEmpty())
            {
                continue;
            }
            JsonObject arguments = new JsonObject();
            Matcher parameter = PARAMETER_PATTERN.matcher(invoke.group(2));
            while (parameter.find())
            {
                String key = parameter.group(1).trim();
                String value = stripModelTokens(parameter.group(2)).trim();
                arguments.add(key, toJsonValue(value));
            }
            McpToolCall call = new McpToolCall();
            call.id = "call_" + UUID.randomUUID().toString().replace("-", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            call.type = "function"; //$NON-NLS-1$
            call.function = new McpToolCallFunctionCall();
            call.function.name = name;
            call.function.arguments = arguments.toString();
            calls.add(call);
        }
        return calls;
    }

    private static String stripModelTokens(String value)
    {
        // MiniMax interleaves an internal segment-delimiter token in its streamed output.
        return value.replace("]<]minimax[>[", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static JsonElement toJsonValue(String value)
    {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return new JsonPrimitive(Boolean.valueOf(Boolean.parseBoolean(value)));
        }
        if (INTEGER_PATTERN.matcher(value).matches())
        {
            try
            {
                return new JsonPrimitive(Long.valueOf(value));
            }
            catch (NumberFormatException e)
            {
                // fall through to string
            }
        }
        if (DECIMAL_PATTERN.matcher(value).matches())
        {
            try
            {
                return new JsonPrimitive(Double.valueOf(value));
            }
            catch (NumberFormatException e)
            {
                // fall through to string
            }
        }
        return new JsonPrimitive(value);
    }


    private CompletableFuture<AskRoundResult> askSingleRound(Session session, String conversationId,
        ConversationAskRequest conversationRequest,
        IObserver<ConversationAskResponse> observer, ICancellationToken cancellationToken)
    {
        var future = new CompletableFuture<AskRoundResult>();
        var optionalRequest =
            requestBuilder.create(settings.getUrl() + "chat_api/v1/conversations/" + conversationId + "/messages"); //$NON-NLS-1$ //$NON-NLS-2$
        if (optionalRequest.isEmpty())
        {
            future.complete(new AskRoundResult());
            return future;
        }
        if (cancellationToken.isCanceled())
        {
            future.complete(new AskRoundResult());
        }
        var requestBuilder = optionalRequest.get().header("Session-Id", session.sessionId); //$NON-NLS-1$
        var requestBody = serializeWithNull(conversationRequest);

        logDebug.trace(TracingSources.API_CALLS, "Request", () -> requestBody); //$NON-NLS-1$

        BodyPublisher bodyPublisher;
        try
        {
            bodyPublisher = BodyPublishers.ofString(requestBody);
        }
        catch (Exception error)
        {
            log.error(error, cancellationToken.toString());
            future.completeExceptionally(error);
            return future;
        }

        var request = requestBuilder.POST(bodyPublisher).build();
        log.request(request, cancellationToken.toString(), requestBody);
        var stopwatch = Stopwatch.createStarted();
        var client = clientBuilder.get();


        var asyncRequest = client.sendAsync(request, BodyHandlers.ofLines());
        var attachToken = CancellationTokenSource.attach(cancellationToken, () -> {
            asyncRequest.cancel(true);
        });

        asyncRequest.orTimeout(settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)
            .thenApplyAsync(response -> log.response(response, cancellationToken.toString(), stopwatch, true, true))
            .thenApplyAsync(HttpResponse::body)
            .thenAcceptAsync(stream -> {
                try
                {
                    AskRoundResult result = processStream(stream, observer, cancellationToken);
                    future.complete(result);
                }
                catch (Exception ex)
                {
                    future.completeExceptionally(ex);
                }
            })
            .whenComplete((r, error) -> {
                try
                {
                    attachToken.close();
                }
                catch (Exception ex)
                {
                    log.error(ex, "Failed to cancel request: " + asyncRequest + " with token: " + attachToken); //$NON-NLS-1$ //$NON-NLS-2$
                }

                if (error != null && !future.isDone())
                {
                    future.completeExceptionally(error);
                }
            });

        return future;
    }

    private String serializeWithNull(ConversationAskRequest conversationRequest)
    {
        var root = new JsonObject();
        if (conversationRequest.parentUuid != null)
        {
            root.addProperty("parent_uuid", conversationRequest.parentUuid); //$NON-NLS-1$
        }
        else
        {
            root.add("parent_uuid", JsonNull.INSTANCE); //$NON-NLS-1$
        }
        root.addProperty("role", conversationRequest.role); //$NON-NLS-1$
        root.add("content", conversationRequest.content); //$NON-NLS-1$
        return root.toString();
    }

    private Optional<ConversationResponse> createConversation(Session session,
        ConversationRequest conversationRequest, ICancellationToken cancellationToken)
        throws InterruptedException, ExecutionException
    {
        var optionalRequest =
            requestBuilder.create(settings.getUrl() + "chat_api/v1/conversations"); //$NON-NLS-1$
        if (optionalRequest.isEmpty())
        {
            return Optional.empty();
        }

        var requestBuilder = optionalRequest.get().header("Session-Id", session.sessionId); //$NON-NLS-1$
        var requestBody = json.serialize(conversationRequest);
        var bodyPublisher = BodyPublishers.ofString(requestBody);
        var request = requestBuilder.POST(bodyPublisher).build();
        log.request(request, cancellationToken.toString(), requestBody);
        var stopwatch = Stopwatch.createStarted();
        return clientBuilder.get()
            .sendAsync(request, BodyHandlers.ofString())
            .orTimeout(settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)
            .thenApplyAsync(response -> log.response(response, null, stopwatch, true, true))
            .thenApplyAsync(HttpResponse::body)
            .thenApplyAsync(content -> json.deserialize(content, ConversationResponse.class))
            .get();
    }

    private boolean isCancellationException(Throwable error)
    {
        return error instanceof CompletionException
            && ((CompletionException)error).getCause() instanceof CancellationException;
    }

    private AskRoundResult processStream(Stream<String> stream, IObserver<ConversationAskResponse> observer,
        ICancellationToken cancellationToken)
    {
        AskRoundResult result = new AskRoundResult();
        AssistantMessageAccumulator accumulator = null;

        for (String line : (Iterable<String>)stream::iterator)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            logDebug.trace(TracingSources.API_CALLS, "Response stream raw", () -> line); //$NON-NLS-1$

            ConversationAskResponse response = parseResponseLine(line);
            if (response == null)
            {
                continue;
            }

            if ("assistant".equals(response.role)) //$NON-NLS-1$
            {
                if (accumulator == null)
                {
                    accumulator = new AssistantMessageAccumulator();
                    accumulator.uuid = response.uuid;
                    accumulator.parentUuid = response.parentUuid;
                    accumulator.role = response.role;
                }

                if (response.contentDelta != null)
                {
                    applyDelta(accumulator, response.contentDelta);
                    observer.onNext(response);
                }

                if (response.finished)
                {
                    if (response.content != null)
                    {
                        applyFinalContent(accumulator, response.content);
                    }

                    ConversationAskResponse finalResponse = accumulator.toResponse();
                    logDebug.trace(TracingSources.API_CALLS, "Response stream final", //$NON-NLS-1$
                        () -> json.serialize(finalResponse));
                    result.lastResponse = finalResponse;
                    return result;
                }

                continue;
            }

            result.lastResponse = response;

            if (hasPayload(response))
            {
                observer.onNext(response);
            }
        }

        return result;
    }

    private ConversationAskResponse parseResponseLine(String line)
    {
        if (line == null || line.isBlank())
        {
            return null;
        }

        var sb = new StringBuilder(line.length() + 2);
        sb.append('{');
        sb.append(line);
        sb.append('}');

        var data = json.deserialize(sb.toString(), ConversationAskResponseStreamData.class);
        if (data.isEmpty() || data.get().data == null)
        {
            return null;
        }
        return data.get().data;
    }

    private void applyDelta(AssistantMessageAccumulator accumulator, AssistantMessageContentDelta delta)
    {
        if (delta.content != null)
        {
            accumulator.content.append(delta.content);
        }

        if (delta.reasoningContent != null)
        {
            accumulator.reasoningContent.append(delta.reasoningContent);
        }

        if (delta.toolCalls != null)
        {
            for (ChoiceDeltaToolCall deltaCall : delta.toolCalls)
            {
                if (deltaCall == null)
                {
                    continue;
                }

                while (accumulator.toolCalls.size() <= deltaCall.index)
                {
                    accumulator.toolCalls.add(new McpToolCall());
                }

                McpToolCall call = accumulator.toolCalls.get(deltaCall.index);

                if (deltaCall.id != null)
                {
                    call.id = deltaCall.id;
                }

                if (call.function == null)
                {
                    call.function = new McpToolCallFunctionCall();
                }

                if (deltaCall.function != null)
                {
                    if (deltaCall.function.name != null)
                    {
                        call.function.name = deltaCall.function.name;
                    }

                    if (deltaCall.function.arguments != null)
                    {
                        String oldArgs = call.function.arguments;
                        call.function.arguments = (oldArgs == null ? "" : oldArgs) + deltaCall.function.arguments; //$NON-NLS-1$
                    }
                }
            }
        }
    }

    private void applyFinalContent(AssistantMessageAccumulator accumulator, AssistantMessageContent content)
    {
        accumulator.content.setLength(0);
        accumulator.reasoningContent.setLength(0);
        accumulator.toolCalls.clear();

        if (content.content != null)
        {
            accumulator.content.append(content.content);
        }
        if (content.reasoningContent != null)
        {
            accumulator.reasoningContent.append(content.reasoningContent);
        }
        if (content.toolCalls != null)
        {
            accumulator.toolCalls.addAll(content.toolCalls);
        }
    }

    private boolean hasPayload(ConversationAskResponse response)
    {
        if (response == null)
        {
            return false;
        }
        if (response.content != null)
        {
            return true;
        }
        return response.contentDelta != null;
    }

    private static class AskRoundResult
    {
        public ConversationAskResponse lastResponse;
    }

    private static class ConversationAskResponseStreamData
    {
        public ConversationAskResponse data;
    }

    private static class AssistantMessageAccumulator
    {
        public String uuid;
        public String parentUuid;
        public String role;
        public StringBuilder content = new StringBuilder();
        public StringBuilder reasoningContent = new StringBuilder();
        public ArrayList<McpToolCall> toolCalls = new ArrayList<>();

        public ConversationAskResponse toResponse()
        {
            var response = new ConversationAskResponse();
            response.uuid = uuid;
            response.parentUuid = parentUuid;
            response.role = role;
            response.finished = true;

            AssistantMessageContent finalContent = new AssistantMessageContent();
            finalContent.content = content.length() > 0 ? content.toString() : null;
            finalContent.reasoningContent = reasoningContent.length() > 0 ? reasoningContent.toString() : null;
            finalContent.toolCalls = toolCalls.isEmpty() ? null : toolCalls;

            response.content = finalContent;
            response.contentDelta = null;
            return response;
        }
    }
}
