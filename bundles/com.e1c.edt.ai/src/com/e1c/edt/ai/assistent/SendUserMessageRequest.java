/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.assistent.model.SkillCompletionPolicy;
import com.google.common.base.Preconditions;

/**
 * @author Bogdan Sushkov
 */
public class SendUserMessageRequest
{
    private final IProject project;
    private final String message;
    private final ConversationSession conversationSession;
    private final boolean forceNewConversation;
    private final String skillName;
    private final Boolean chat;
    private final Integer maxToolRounds;
    private final Set<String> allowedTools;
    private final SkillCompletionPolicy completionPolicy;

    public SendUserMessageRequest(IProject project, String message, ConversationSession conversationSession,
        boolean forceNewConversation)
    {
        this(project, message, conversationSession, forceNewConversation, null, null, null, null, null);
    }

    /**
     * @param skillName optional skill override for the (new) conversation; {@code null} keeps the default
     * @param chat optional {@code is_chat} override; {@code null} keeps the default
     * @param maxToolRounds optional tool-round cap override; {@code null} keeps the default
     */
    public SendUserMessageRequest(IProject project, String message, ConversationSession conversationSession,
        boolean forceNewConversation, String skillName, Boolean chat, Integer maxToolRounds)
    {
        this(project, message, conversationSession, forceNewConversation, skillName, chat, maxToolRounds, null, null);
    }

    /**
     * @param skillName optional skill override for the (new) conversation; {@code null} keeps the default
     * @param chat optional {@code is_chat} override; {@code null} keeps the default
     * @param maxToolRounds optional tool-round cap override; {@code null} keeps the default
     * @param allowedTools optional tool allowlist; {@code null} exposes all available tools
     */
    public SendUserMessageRequest(IProject project, String message, ConversationSession conversationSession,
        boolean forceNewConversation, String skillName, Boolean chat, Integer maxToolRounds,
        Set<String> allowedTools)
    {
        this(project, message, conversationSession, forceNewConversation, skillName, chat, maxToolRounds,
            allowedTools, null);
    }

    public SendUserMessageRequest(IProject project, String message, ConversationSession conversationSession,
        boolean forceNewConversation, String skillName, Boolean chat, Integer maxToolRounds,
        Set<String> allowedTools, SkillCompletionPolicy completionPolicy)
    {
        this.project = Preconditions.checkNotNull(project);
        this.message = message;
        this.conversationSession = conversationSession;
        this.forceNewConversation = forceNewConversation;
        this.skillName = skillName;
        this.chat = chat;
        this.maxToolRounds = maxToolRounds;
        this.allowedTools = allowedTools == null ? null
            : Collections.unmodifiableSet(new LinkedHashSet<>(allowedTools));
        this.completionPolicy = completionPolicy;
    }

    /**
     * @return skill override or {@code null} to use the default
     */
    public String getSkillName()
    {
        return skillName;
    }

    /**
     * @return is_chat override or {@code null} to use the default
     */
    public Boolean getChat()
    {
        return chat;
    }

    /**
     * @return tool-round cap override or {@code null} to use the default
     */
    public Integer getMaxToolRounds()
    {
        return maxToolRounds;
    }

    /**
     * @return the tool allowlist, or an empty optional when all tools are available
     */
    public Optional<Set<String>> getAllowedTools()
    {
        return Optional.ofNullable(allowedTools);
    }

    public Optional<SkillCompletionPolicy> getCompletionPolicy()
    {
        return Optional.ofNullable(completionPolicy);
    }

    public IProject getProject()
    {
        return project;
    }

    public String getMessage()
    {
        return message;
    }

    public ConversationSession getConversationSession()
    {
        return conversationSession;
    }

    public boolean isForceNewConversation()
    {
        return forceNewConversation;
    }
}
