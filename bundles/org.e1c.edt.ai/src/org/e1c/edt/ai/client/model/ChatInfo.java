/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client.model;

import java.net.URL;

/**
 * This class contains information of created chat.
 * It uses to provide connection with active chat.
 * It keeps info about user's token, active chat UUID and UUID
 * of last message, appeared in chat.
 * @author Bogdan Sushkov
 */
public class ChatInfo
{
    private String conversationUUID = null;
    private String parentUUID = null;
    private String clientToken = null;
    private final URL url;

    /**
     * Constructs object ChatInfo
     * @param token
     * @param URL
     */
    public ChatInfo(String token, URL url)
    {
        clientToken = token;
        this.url = url;
    }

    /**
     * Set <code>UUID</code> of active conversation.
     * @param UUID
     */
    public void setConversationUUID(String conversationUUID)
    {
        this.conversationUUID = conversationUUID;
    }

    /**
    * Set client's token.
    * @param UUID
    */
    public void setClientToken(String clientToken)
    {
        this.clientToken = clientToken;
    }

    /**
     * Returns UUID of openned chat. Unless chat have been openned,
     * <b>null</b> will be returned.
     * @return UUID string or null, if chat have not been created yet
     */
    public String getConversationUUID()
    {
        return conversationUUID;
    }

    /**
    * Returns client's token.
    * @return clientID
    */
    public String getClientToken()
    {
        return clientToken;
    }

    /**
     * Returns <code>UUID</code> of last message, appeared in chat.
     * @return the parentUUID
     */
    public String getParentUUID()
    {
        return parentUUID;
    }

    /**
     * Set UUID of last message, appeared in chat.
     * @param parentUUID the parentUUID to set
     */
    public void setParentUUID(String parentUUID)
    {
        this.parentUUID = parentUUID;
    }

    /**
     * @return the uRL
     */
    public URL getURL()
    {
        return url;
    }
}
