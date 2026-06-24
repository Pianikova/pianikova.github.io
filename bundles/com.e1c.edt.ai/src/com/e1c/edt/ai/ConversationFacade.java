/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import java.util.ArrayList;
import java.util.List;
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
import com.e1c.edt.ai.assistent.model.ProjectId;
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
    private final IConversations conversations;
    private final IJson json;
    private final IMcpTools mcpTools;
    private final ISettings settings;

    @Inject
    public ConversationFacade(IConversations conversations, IJson json, IMcpTools mcpTools, ISettings settings)
    {
        Preconditions.checkNotNull(conversations);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(mcpTools);
        Preconditions.checkNotNull(settings);
        this.settings = settings;
        this.conversations = conversations;
        this.json = json;
        this.mcpTools = mcpTools;
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
        ICancellationToken cancellationToken)
    {
        boolean isNewConversation = request.isForceNewConversation() || request.getConversationSession() == null
            || request.getConversationSession().isNewConversation();

        var conversationFuture = isNewConversation ? createConversationAsync(request, cancellationToken)
            : CompletableFuture.completedFuture(request.getConversationSession().getConversationId());
        return conversationFuture.thenCompose(conversationId -> {
            var parentUuid = isNewConversation ? null : request.getConversationSession().getReplyToMessageUuid();

            ConversationAskRequest askRequest = createAskRequest(request.getMessage(), parentUuid);
            if (request.getMaxToolRounds() != null)
            {
                askRequest.maxToolRounds = request.getMaxToolRounds().intValue();
            }
            return collectAssistantResult(request.getProjectId(), conversationId, askRequest, cancellationToken);
        });
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
        conversationRequest.skillName = request.getSkillName() != null ? request.getSkillName() : "raw"; //$NON-NLS-1$
        conversationRequest.uiLanguage = settings.getLanguage();
        conversationRequest.programmingLanguage = "1c"; //$NON-NLS-1$
        // Default false so dev/helper conversations are not listed as chats; the dev-autopilot
        // may override this to mirror the interactive chat (is_chat=true).
        conversationRequest.isChat = request.getChat() != null ? request.getChat().booleanValue() : false;
        conversationRequest.scriptLanguage = settings.getLanguage();

        return conversations.createConversationAsync(request.getProjectId(), conversationRequest, cancellationToken)
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
    private ConversationAskRequest createAskRequest(String instruction, String parentUuid)
    {
        var skillContent = new JsonObject();
        skillContent.addProperty("instruction", instruction); //$NON-NLS-1$
        skillContent.add("code", new JsonArray()); //$NON-NLS-1$

        ConversationRequestContent requestContent = new ConversationRequestContent();
        requestContent.content = skillContent;
        requestContent.tools = getToolsDefinitions();

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
    private List<ToolDefinition> getToolsDefinitions()
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
            var tool = new ToolDefinition();
            tool.name = func.name;
            tool.description = func.description;
            tool.parameters =
                json.deserialize(json.serialize(func.parameters), JsonElement.class).orElse(null);

            toolDefinitions.add(tool);
        }
        return toolDefinitions;
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
    private CompletableFuture<SendMessageResult> collectAssistantResult(ProjectId projectId, String conversationUuid,
        ConversationAskRequest askRequest, ICancellationToken cancellationToken)
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

        conversations.createAskSource(projectId, conversationUuid, askRequest, cancellationToken)
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
                    }

                    // 2) дельты
                    if (value.contentDelta != null && value.contentDelta.content != null)
                    {
                        String uuid = currentAssistantUuid.get();
                        if (uuid != null)
                        {
                            textByMessageUuid.computeIfAbsent(uuid, k -> new StringBuilder())
                                .append(value.contentDelta.content);
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
