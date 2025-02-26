/*
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.client.model;

/**
 * This class stores UUID of chat
 * @author Bogdan Sushkov
 *
 */
public class ConversationID
{
    private String UUID;

    public ConversationID(String UUID)
    {
        this.UUID = UUID;
    }

    /**
     *
     * @param UUID
     */
    public void setUUID(String UUID)
    {
        this.UUID = UUID;
    }

    /**
     * Returns UUID of chat
     * @return the UUID
     */
    public String getUUID()
    {
        return UUID;
    }
}
