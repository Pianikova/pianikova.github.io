/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.assistent.model.ProjectId;

/**
 * @author Bogdan Sushkov
 */
public class SendUserMessageRequest
{
    private final ProjectId projectId;
    private final String message;
    private final ConversationSession conversationSession;
    private final boolean forceNewConversation;
    private final String skillName;
    private final Boolean chat;
    private final Integer maxToolRounds;

    public SendUserMessageRequest(ProjectId projectId, String message, ConversationSession conversationSession,
        boolean forceNewConversation)
    {
        this(projectId, message, conversationSession, forceNewConversation, null, null, null);
    }

    /**
     * @param skillName optional skill override for the (new) conversation; {@code null} keeps the default
     * @param chat optional {@code is_chat} override; {@code null} keeps the default
     * @param maxToolRounds optional tool-round cap override; {@code null} keeps the default
     */
    public SendUserMessageRequest(ProjectId projectId, String message, ConversationSession conversationSession,
        boolean forceNewConversation, String skillName, Boolean chat, Integer maxToolRounds)
    {
        this.projectId = projectId;
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

    public ProjectId getProjectId()
    {
        return projectId;
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
