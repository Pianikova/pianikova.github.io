/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.util.Map;

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
    private String skillId;
    private Map<String, Object> skillParameters;

    public SendUserMessageRequest(ProjectId projectId, String message, ConversationSession conversationSession,
        boolean forceNewConversation)
    {
        this(projectId, message, conversationSession, forceNewConversation, null, Map.of());
    }

    public SendUserMessageRequest(ProjectId projectId, String message, ConversationSession conversationSession,
        boolean forceNewConversation, String skillId, Map<String, Object> skillParameters)
    {
        this.projectId = projectId;
        this.message = message;
        this.conversationSession = conversationSession;
        this.forceNewConversation = forceNewConversation;
        this.skillId = skillId;
        this.skillParameters = skillParameters;
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

    public String getSkillId()
    {
        return skillId;
    }

    public Map<String, Object> getSkillParameters()
    {
        return Map.copyOf(skillParameters);
    }

    public boolean isSkillRequest()
    {
        return skillId != null && !skillId.isBlank();
    }
}
