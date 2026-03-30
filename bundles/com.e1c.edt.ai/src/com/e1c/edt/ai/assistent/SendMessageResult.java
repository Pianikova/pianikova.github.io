/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

/**
 * @author Bogdan Sushkov
 *
 */
public class SendMessageResult
{
    private final String text;
    private final ConversationSession session;

    public SendMessageResult(String text, ConversationSession session)
    {
        this.text = text;
        this.session = session;
    }

    public String getText()
    {
        return text;
    }

    public ConversationSession getSession()
    {
        return session;
    }
}
