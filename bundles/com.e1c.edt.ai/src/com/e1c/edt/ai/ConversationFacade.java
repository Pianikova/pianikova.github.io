/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.e1c.edt.ai.assistent.ConversationSession;
import com.e1c.edt.ai.assistent.IConversations;
import com.e1c.edt.ai.assistent.SendMessageResult;
import com.e1c.edt.ai.assistent.SendUserMessageRequest;
import com.e1c.edt.ai.assistent.model.ConversationAskRequest;
import com.e1c.edt.ai.assistent.model.ConversationAskResponse;
import com.e1c.edt.ai.assistent.model.ConversationRequest;
import com.e1c.edt.ai.assistent.model.ConversationRequestContent;
import org.eclipse.core.resources.IProject;
import com.e1c.edt.ai.assistent.model.SkillCompletionPolicy;
import com.e1c.edt.ai.assistent.model.ToolDefinition;
import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

/**
 * Фасад для управления диалогами с AI-ассистентом в среде разработки EDT.
 * <p>
 * Предоставляет возможность отправлять сообщения ассистенту, создавать новые диалоги,
 * выполнять скиллы перед отправкой сообщения, обрабатывать потоковые ответы ассистента.
 * Интегрируется с MCP-инструментами для предоставления ассистенту контекста работы.
 * </p>
 *
 * @author Bogdan Sushkov
 */
public class ConversationFacade
    implements IConversationFacade
{
    private static final int MAX_SKILL_COMPLETION_CONTINUES = 3;
    private static final int MAX_SKILL_COMPLETION_RESTARTS = 1;

    private final IConversations conversations;
    private final IJson json;
    private final IMcpTools mcpTools;
    private final ISettings settings;
    private final ILog log;

    @Inject
    public ConversationFacade(IConversations conversations, IJson json, IMcpTools mcpTools, ISettings settings,
        ILog log)
    {
        Preconditions.checkNotNull(conversations);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(mcpTools);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(log);
        this.settings = settings;
        this.conversations = conversations;
        this.json = json;
        this.mcpTools = mcpTools;
        this.log = log;
    }

    /**
     * Отправляет сообщение в диалог.
     * <p>
     * Определяет необходимость создания нового диалога на основе флага {@code forceNewConversation}
     * или состояния сессии. Создаёт диалог при необходимости, формирует запрос к ассистенту
     * с инструкцией и инструментами, затем собирает ответ.
     * </p>
     *
     * @param request подготовленный запрос пользователя
     * @param cancellationToken токен для отмены операции
     * @return {@link CompletableFuture} с результатом отправки
     */
    @Override
    public CompletableFuture<SendMessageResult> sendAsync(SendUserMessageRequest request,
        ICancellationToken cancellationToken, IConversationProgressListener progressListener)
    {
        boolean isNewConversation = request.isForceNewConversation() || request.getConversationSession() == null
            || request.getConversationSession().isNewConversation();

        var conversationFuture = isNewConversation ? createConversationAsync(request, cancellationToken)
            : CompletableFuture.completedFuture(request.getConversationSession().getConversationId());
        return conversationFuture.thenCompose(conversationId -> {
            var parentUuid = isNewConversation ? null : request.getConversationSession().getReplyToMessageUuid();

            var instruction = withCompletionProtocol(request.getMessage(),
                request.getCompletionPolicy().orElse(null));
            ConversationAskRequest askRequest =
                createAskRequest(instruction, parentUuid, request.getAllowedTools().orElse(null));
            if (request.getMaxToolRounds() != null)
            {
                askRequest.maxToolRounds = request.getMaxToolRounds().intValue();
            }
            return collectAssistantResult(request.getProject(), conversationId, askRequest, cancellationToken,
                progressListener).thenCompose(result -> enforceCompletionPolicy(request, conversationId, result,
                    cancellationToken, progressListener, 0, 0, result.getAssistantMessageCount()));
        });
    }

    public static String withCompletionProtocol(String prompt, SkillCompletionPolicy policy)
    {
        if (policy == null)
        {
            return prompt;
        }
        return prompt + "\n\n## Служебный протокол завершения\n\n" //$NON-NLS-1$
            + "Только когда задача полностью завершена, добавь отдельной последней строкой `" //$NON-NLS-1$
            + policy.getMarker() + "`. Не добавляй этот маркер к промежуточному ответу или вместо " //$NON-NLS-1$
            + "настоящего вызова инструмента."; //$NON-NLS-1$
    }

    private CompletableFuture<SendMessageResult> enforceCompletionPolicy(SendUserMessageRequest request,
        String conversationId, SendMessageResult result, ICancellationToken cancellationToken,
        IConversationProgressListener progressListener, int continuation, int restart,
        int assistantMessageCount)
    {
        var policy = request.getCompletionPolicy().orElse(null);
        if (policy == null || result == null)
        {
            return CompletableFuture.completedFuture(result);
        }

        var finalText = stripCompletionMarker(result.getText(), policy.getMarker());
        var markerPresent = finalText != null;
        var candidate = markerPresent ? finalText : result.getText();
        var invalidJson = policy.isRejectToolLikeJson() && isJsonObject(candidate);
        if (markerPresent && !invalidJson)
        {
            return CompletableFuture.completedFuture(new SendMessageResult(candidate, result.getSession(),
                result.getReasoning(), assistantMessageCount));
        }

        if (continuation >= MAX_SKILL_COMPLETION_CONTINUES)
        {
            if (restart < MAX_SKILL_COMPLETION_RESTARTS)
            {
                return restartCompletionConversation(request, cancellationToken, progressListener, restart + 1,
                    assistantMessageCount);
            }
            return failedFuture(new IllegalStateException(
                "Skill did not produce a valid final answer after completion retries and restart")); //$NON-NLS-1$
        }
        if (result.getSession() == null || result.getSession().getReplyToMessageUuid() == null)
        {
            return failedFuture(new IllegalStateException(
                "Skill completion response has no conversation session")); //$NON-NLS-1$
        }

        var allowedTools = request.getAllowedTools().orElse(null);
        var exactToolHint = invalidJson && allowedTools != null && allowedTools.size() == 1
            ? " Вызови единственный доступный инструмент `" + allowedTools.iterator().next() //$NON-NLS-1$
                + "` через настоящий function call с уже указанными параметрами. Не повторяй JSON текстом." //$NON-NLS-1$
            : ""; //$NON-NLS-1$
        var reason = invalidJson
            ? "Предыдущий ответ является JSON-текстом, похожим на параметры инструмента, и не является допустимым финальным ответом." //$NON-NLS-1$
                + exactToolHint + " " //$NON-NLS-1$
            : "Предыдущий ответ не содержит обязательный маркер завершения. "; //$NON-NLS-1$
        var instruction = reason
            + "Продолжи выполнение исходной задачи в этой же беседе. Если нужны данные, сделай настоящий function call доступного инструмента, не печатай его параметры текстом. " //$NON-NLS-1$
            + "Только после полного завершения задачи добавь отдельной последней строкой `" //$NON-NLS-1$
            + policy.getMarker() + "`."; //$NON-NLS-1$

        var askRequest = createAskRequest(instruction, result.getSession().getReplyToMessageUuid(),
            request.getAllowedTools().orElse(null));
        if (request.getMaxToolRounds() != null)
        {
            askRequest.maxToolRounds = request.getMaxToolRounds().intValue();
        }
        return collectAssistantResult(request.getProject(), conversationId, askRequest, cancellationToken,
            progressListener).thenCompose(next -> enforceCompletionPolicy(request, conversationId, next,
                cancellationToken, progressListener, continuation + 1, restart,
                assistantMessageCount + next.getAssistantMessageCount()));
    }

    private CompletableFuture<SendMessageResult> restartCompletionConversation(SendUserMessageRequest request,
        ICancellationToken cancellationToken, IConversationProgressListener progressListener, int restart,
        int assistantMessageCount)
    {
        return createConversationAsync(request, cancellationToken).thenCompose(conversationId -> {
            var instruction = withCompletionProtocol(request.getMessage(),
                request.getCompletionPolicy().orElse(null));
            var askRequest = createAskRequest(instruction, null, request.getAllowedTools().orElse(null));
            if (request.getMaxToolRounds() != null)
            {
                askRequest.maxToolRounds = request.getMaxToolRounds().intValue();
            }
            return collectAssistantResult(request.getProject(), conversationId, askRequest, cancellationToken,
                progressListener).thenCompose(result -> enforceCompletionPolicy(request, conversationId, result,
                    cancellationToken, progressListener, 0, restart,
                    assistantMessageCount + result.getAssistantMessageCount()));
        });
    }

    private boolean isJsonObject(String text)
    {
        if (text == null || text.isBlank())
        {
            return false;
        }
        return json.deserialize(text.trim(), JsonElement.class).map(JsonElement::isJsonObject).orElse(false);
    }

    /**
     * Removes a marker only when it occupies the final non-blank line.
     *
     * @return text without the marker, or {@code null} when the marker is absent
     */
    public static String stripCompletionMarker(String text, String marker)
    {
        if (text == null)
        {
            return null;
        }
        var normalized = text.replace("\r\n", "\n").replace('\r', '\n').stripTrailing(); //$NON-NLS-1$ //$NON-NLS-2$
        var markerStart = normalized.length() - marker.length();
        if (markerStart < 0 || !normalized.regionMatches(markerStart, marker, 0, marker.length())
            || markerStart > 0 && normalized.charAt(markerStart - 1) != '\n')
        {
            return null;
        }
        return normalized.substring(0, markerStart).stripTrailing();
    }

    /**
     * Асинхронно создаёт новый диалог с настройками по умолчанию.
     * <p>
     * Использует настройки языка из {@link ISettings}, программный язык "1c".
     * Диалог создаётся как скрытый (не отображается в списке чатов).
     * </p>
     *
     * @param projectId идентификатор проекта
     * @param cancellationToken токен для отмены операции
     * @return {@link CompletableFuture} с UUID созданного диалога
     */
    private CompletableFuture<String> createConversationAsync(SendUserMessageRequest request,
        ICancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled())
        {
            return CompletableFuture.failedFuture(new CancellationException("Cancelled")); //$NON-NLS-1$
        }
        ConversationRequest conversationRequest = new ConversationRequest();
        conversationRequest.skillName = request.getSkillName() != null ? request.getSkillName() : "edt"; //$NON-NLS-1$
        conversationRequest.uiLanguage = settings.getLanguage();
        conversationRequest.programmingLanguage = "1c"; //$NON-NLS-1$
        // Default false so dev/helper conversations are not listed as chats; the dev-autopilot
        // may override this to mirror the interactive chat (is_chat=true).
        conversationRequest.isChat = request.getChat() != null ? request.getChat().booleanValue() : false;
        conversationRequest.scriptLanguage = settings.getLanguage();

        return conversations.createConversationAsync(request.getProject(), conversationRequest, cancellationToken)
            .thenCompose(optionalResponse -> {
                if (optionalResponse.isEmpty() || optionalResponse.get().uuid == null
                    || optionalResponse.get().uuid.isBlank())
                {
                    return failedFuture(new IllegalArgumentException("Failed to create conversation")); //$NON-NLS-1$
                }
                return CompletableFuture.completedFuture(optionalResponse.get().uuid);
            });
    }


    /**
     * Формирует запрос к ассистенту с инструкцией и инструментами.
     * <p>
     * Создаёт структуру JSON с инструкцией и пустым массивом кода,
     * добавляет определения всех доступных MCP-инструментов.
     * </p>
     *
     * @param instruction текст инструкции для ассистента
     * @param parentUuid UUID родительского сообщения (для продолжения диалога) или null
     * @return сформированный {@link ConversationAskRequest}
     */
    private ConversationAskRequest createAskRequest(String instruction, String parentUuid, Set<String> allowedTools)
    {
        var skillContent = new JsonObject();
        skillContent.addProperty("instruction", instruction); //$NON-NLS-1$
        skillContent.add("code", new JsonArray()); //$NON-NLS-1$

        ConversationRequestContent requestContent = new ConversationRequestContent();
        requestContent.content = skillContent;
        requestContent.tools = getToolsDefinitions(allowedTools);

        ConversationAskRequest askRequest = new ConversationAskRequest();
        askRequest.parentUuid = parentUuid;
        askRequest.role = "user"; //$NON-NLS-1$
        askRequest.content = json.deserialize(json.serialize(requestContent), JsonElement.class).orElse(null);
        return askRequest;
    }

    /**
     * Получает определения всех доступных MCP-инструментов.
     * <p>
     * Запрашивает спецификации инструментов через {@link IMcpTools}, фильтрует
     * инструменты без имени, преобразует в {@link ToolDefinition}.
     * </p>
     *
     * @return список определений инструментов
     */
    private List<ToolDefinition> getToolsDefinitions(Set<String> allowedTools)
    {
        ArrayList<ToolDefinition> toolDefinitions = new ArrayList<>();
        var functions = mcpTools.getSpecifications().join().stream().map(i -> i.function).collect(Collectors.toList());
        if (functions == null)
        {
            return toolDefinitions;
        }
        for (var func : functions)
        {
            if (func.name == null || func.name.isBlank())
            {
                continue;
            }
            if (allowedTools != null && !allowedTools.contains(func.name))
            {
                continue;
            }
            var tool = new ToolDefinition();
            tool.name = func.name;
            tool.description = func.description;
            tool.parameters =
                json.deserialize(json.serialize(func.parameters), JsonElement.class).orElse(null);

            toolDefinitions.add(tool);
        }
        if (allowedTools != null)
        {
            var unknownTools = new LinkedHashSet<>(allowedTools);
            toolDefinitions.stream().map(tool -> tool.name).forEach(unknownTools::remove);
            if (!unknownTools.isEmpty())
            {
                throw new IllegalArgumentException("Unknown tools in allowed-tools: " + unknownTools); //$NON-NLS-1$
            }
        }
        return toolDefinitions;
    }

    /**
     * Reports progress, shielding the response stream from a misbehaving listener: a listener that
     * throws must not abort the conversation it is only observing.
     */
    private void reportProgress(IConversationProgressListener progressListener, int round, int charactersReceived)
    {
        if (progressListener == null)
        {
            return;
        }

        try
        {
            progressListener.onProgress(round, charactersReceived);
        }
        catch (RuntimeException error)
        {
            log.logError(error);
        }
    }

    /**
     * Подписывается на поток ответов ассистента и собирает полный результат.
     * <p>
     * Обрабатывает три типа событий:
     * <ul>
     *   <li>Начало сообщения ассистента - создаёт буфер для текста</li>
     *   <li>Дельты (поступающие части текста) - добавляет в буфер</li>
     *   <li>Завершение сообщения - фиксирует финальный текст</li>
     * </ul>
     * При завершении потока возвращает результат с текстом последнего сообщения
     * и сессией диалога для продолжения.
     * </p>
     *
     * @param projectId идентификатор проекта
     * @param conversationUuid UUID диалога
     * @param askRequest запрос к ассистенту
     * @param cancellationToken токен для отмены операции
     * @return {@link CompletableFuture} с результатом, содержащим текст ответа и сессию
     */
    private CompletableFuture<SendMessageResult> collectAssistantResult(IProject project, String conversationUuid,
        ConversationAskRequest askRequest, ICancellationToken cancellationToken,
        IConversationProgressListener progressListener)
    {
        CompletableFuture<SendMessageResult> future = new CompletableFuture<>();

        AtomicReference<String> lastFinishedAssistantUuid = new AtomicReference<>();
        AtomicReference<String> currentAssistantUuid = new AtomicReference<>();
        AtomicReference<String> finalText = new AtomicReference<>(""); //$NON-NLS-1$
        AtomicReference<String> finalReasoning = new AtomicReference<>(""); //$NON-NLS-1$
        java.util.concurrent.atomic.AtomicInteger assistantMessageCount =
            new java.util.concurrent.atomic.AtomicInteger(0);
        ConcurrentHashMap<String, StringBuilder> textByMessageUuid = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, StringBuilder> reasoningByMessageUuid = new ConcurrentHashMap<>();

        conversations.createAskSource(project, conversationUuid, askRequest, cancellationToken, progressListener)
            .subscribe(new IObserver<ConversationAskResponse>()
            {
                @Override
                public void onNext(ConversationAskResponse value)
                {
                    if (value == null)
                    {
                        return;
                    }

                    // 1) старт assistant-сообщения
                    if ("assistant".equals(value.role) && value.uuid != null && !value.uuid.isBlank()) //$NON-NLS-1$
                    {
                        currentAssistantUuid.set(value.uuid);
                        textByMessageUuid.computeIfAbsent(value.uuid, k -> new StringBuilder());
                        reportProgress(progressListener, assistantMessageCount.get() + 1, 0);
                    }

                    // 2) дельты
                    if (value.contentDelta != null && value.contentDelta.content != null)
                    {
                        String uuid = currentAssistantUuid.get();
                        if (uuid != null)
                        {
                            var text = textByMessageUuid.computeIfAbsent(uuid, k -> new StringBuilder())
                                .append(value.contentDelta.content);
                            reportProgress(progressListener, assistantMessageCount.get() + 1, text.length());
                        }
                    }

                    // 2b) дельты reasoning_content
                    if (value.contentDelta != null && value.contentDelta.reasoningContent != null)
                    {
                        String uuid = currentAssistantUuid.get();
                        if (uuid != null)
                        {
                            reasoningByMessageUuid.computeIfAbsent(uuid, k -> new StringBuilder())
                                .append(value.contentDelta.reasoningContent);
                        }
                    }

                    // 3) финальный assistant packet
                    if (value.finished && "assistant".equals(value.role) //$NON-NLS-1$
                        && value.uuid != null && !value.uuid.isBlank())
                    {
                        lastFinishedAssistantUuid.set(value.uuid);
                        assistantMessageCount.incrementAndGet();

                        String text = null;
                        if (value.content != null && value.content.content != null)
                        {
                            text = value.content.content;
                        }
                        else
                        {
                            StringBuilder sb = textByMessageUuid.get(value.uuid);
                            text = sb == null ? "" : sb.toString(); //$NON-NLS-1$
                        }

                        finalText.set(text);

                        String reasoning = null;
                        if (value.content != null && value.content.reasoningContent != null)
                        {
                            reasoning = value.content.reasoningContent;
                        }
                        else
                        {
                            StringBuilder sb = reasoningByMessageUuid.get(value.uuid);
                            reasoning = sb == null ? "" : sb.toString(); //$NON-NLS-1$
                        }
                        finalReasoning.set(reasoning);
                    }
                }

                @Override
                public void onError(Throwable error)
                {
                    future.completeExceptionally(error);
                }

                @Override
                public void onCompleted()
                {
                    String replyToUuid = lastFinishedAssistantUuid.get();

                    ConversationSession session = new ConversationSession(conversationUuid, replyToUuid);

                    future.complete(new SendMessageResult(finalText.get(), session, finalReasoning.get(),
                        assistantMessageCount.get()));
                }
            });

        return future;
    }


    /**
     * Создаёт неуспешное {@link CompletableFuture} с исключением.
     *
     * @param throwable исключение для завершения
     * @param <T> тип результата
     * @return CompletableFuture, завершённый exceptionally
     */
    private <T> CompletableFuture<T> failedFuture(Throwable throwable)
    {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

}
