/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.assistent.model.ProjectId;

/**
 * @author Bogdan Sushkov
 *
 */
public class SendUserMessageRequest
{
    private final ProjectId projectId;
    private final String message;
    private final ConversationSession conversationSession;
    private final boolean forceNewConversation;

    public SendUserMessageRequest(ProjectId projectId, String message, ConversationSession conversationSession,
        boolean forceNewConversation)
    {
        this.projectId = projectId;
        this.message = message;
        this.conversationSession = conversationSession;
        this.forceNewConversation = forceNewConversation;
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
