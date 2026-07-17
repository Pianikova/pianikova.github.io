/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import org.eclipse.core.resources.IProject;

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

    public SendUserMessageRequest(IProject project, String message, ConversationSession conversationSession,
        boolean forceNewConversation)
    {
        this(project, message, conversationSession, forceNewConversation, null, null, null);
    }

    /**
     * @param skillName optional skill override for the (new) conversation; {@code null} keeps the default
     * @param chat optional {@code is_chat} override; {@code null} keeps the default
     * @param maxToolRounds optional tool-round cap override; {@code null} keeps the default
     */
    public SendUserMessageRequest(IProject project, String message, ConversationSession conversationSession,
        boolean forceNewConversation, String skillName, Boolean chat, Integer maxToolRounds)
    {
        this.project = Preconditions.checkNotNull(project);
        this.message = message;
        this.conversationSession = conversationSession;
        this.forceNewConversation = forceNewConversation;
        this.skillName = skillName;
        this.chat = chat;
        this.maxToolRounds = maxToolRounds;
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
