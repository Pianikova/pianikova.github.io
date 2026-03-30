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
 * @author Bogdan Sushkov
 *
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

    @Override
    public CompletableFuture<SendMessageResult> sendAsync(SendUserMessageRequest request,
        ICancellationToken cancellationToken)
    {
        boolean isNewConversation = request.isForceNewConversation() || request.getConversationSession() == null
            || request.getConversationSession().isNewConversation();

        var conversationFuture = isNewConversation ? createConversationAsync(request.getProjectId(), cancellationToken)
            : CompletableFuture.completedFuture(request.getConversationSession().getConversationId());
        return conversationFuture.thenCompose(conversationId -> {
            var parentUuid = isNewConversation ? null : request.getConversationSession().getReplyToMessageUuid();

            ConversationAskRequest askRequest = createAskRequest(request.getMessage(), parentUuid);
            return collectAssistantResult(request.getProjectId(), conversationId, askRequest, cancellationToken);
        });
    }

    private CompletableFuture<String> createConversationAsync(ProjectId projectId, ICancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled())
        {
            return CompletableFuture.failedFuture(new CancellationException("Cancelled")); //$NON-NLS-1$
        }
        ConversationRequest conversationRequest = new ConversationRequest();
        conversationRequest.skillName = "custom"; //$NON-NLS-1$
        conversationRequest.uiLanguage = settings.getLanguage();
        conversationRequest.programmingLanguage = "1c"; //$NON-NLS-1$
        conversationRequest.isChat = false; /* иначе отображается в списке чатов */
        conversationRequest.scriptLanguage = settings.getLanguage();

        return conversations.createConversationAsync(projectId, conversationRequest, cancellationToken)
            .thenCompose(optionalResponse -> {
                if (optionalResponse.isEmpty() || optionalResponse.get().uuid == null
                    || optionalResponse.get().uuid.isBlank())
                {
                    return failedFuture(new IllegalArgumentException("Failed to create conversation")); //$NON-NLS-1$
                }
                return CompletableFuture.completedFuture(optionalResponse.get().uuid);
            });
    }


    private ConversationAskRequest createAskRequest(String instruction, String parentUuid)
    {
        var skillContent = new JsonObject();
        skillContent.addProperty("instruction", instruction); //$NON-NLS-1$
        skillContent.add("code", new JsonArray()); //$NON-NLS-1$

        ConversationRequestContent requestContent = new ConversationRequestContent();
        requestContent.content = json.deserialize(json.serialize(skillContent), JsonElement.class).orElse(null);
        requestContent.tools = getToolsDefinitions();

        ConversationAskRequest askRequest = new ConversationAskRequest();
        askRequest.parentUuid = parentUuid;
        askRequest.role = "user"; //$NON-NLS-1$
        askRequest.content = json.deserialize(json.serialize(requestContent), JsonElement.class).orElse(null);
        return askRequest;
    }

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

    private CompletableFuture<SendMessageResult> collectAssistantResult(ProjectId projectId, String conversationUuid,
        ConversationAskRequest askRequest, ICancellationToken cancellationToken)
    {
        CompletableFuture<SendMessageResult> future = new CompletableFuture<>();

        AtomicReference<String> lastFinishedAssistantUuid = new AtomicReference<>();
        AtomicReference<String> currentAssistantUuid = new AtomicReference<>();
        AtomicReference<String> finalText = new AtomicReference<>(""); //$NON-NLS-1$
        ConcurrentHashMap<String, StringBuilder> textByMessageUuid = new ConcurrentHashMap<>();

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

                    // 3) финальный assistant packet
                    if (value.finished && "assistant".equals(value.role) //$NON-NLS-1$
                        && value.uuid != null && !value.uuid.isBlank())
                    {
                        lastFinishedAssistantUuid.set(value.uuid);

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

                    future.complete(new SendMessageResult(finalText.get(), session));
                }
            });

        return future;
    }


    private <T> CompletableFuture<T> failedFuture(Throwable throwable)
    {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

}
