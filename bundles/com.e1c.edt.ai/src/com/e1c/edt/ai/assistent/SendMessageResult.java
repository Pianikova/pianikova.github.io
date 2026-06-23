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
    private final String reasoning;
    private final int assistantMessageCount;

    public SendMessageResult(String text, ConversationSession session)
    {
        this(text, session, null, 0);
    }

    public SendMessageResult(String text, ConversationSession session, String reasoning, int assistantMessageCount)
    {
        this.text = text;
        this.session = session;
        this.reasoning = reasoning;
        this.assistantMessageCount = assistantMessageCount;
    }

    public String getText()
    {
        return text;
    }

    public ConversationSession getSession()
    {
        return session;
    }

    /**
     * @return concatenated {@code reasoning_content} of the final assistant message, or {@code null}
     */
    public String getReasoning()
    {
        return reasoning;
    }

    /**
     * @return number of finished assistant messages (model turns) in the conversation stream
     */
    public int getAssistantMessageCount()
    {
        return assistantMessageCount;
    }
}
