/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

/**
 * @author Bogdan Sushkov
 *
 */
public class ConversationSession
{
    private final String conversationId;
    private final String replyToMessageUuid;

    public ConversationSession(String conversationId, String replyToMessageUuid)
    {
        this.conversationId = conversationId;
        this.replyToMessageUuid = replyToMessageUuid;
    }

    public String getConversationId()
    {
        return conversationId;
    }

    public String getReplyToMessageUuid()
    {
        return replyToMessageUuid;
    }

    public boolean isNewConversation()
    {
        return conversationId == null || conversationId.isBlank();
    }
}
