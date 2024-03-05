/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client;

import java.util.ArrayList;

import org.e1c.edt.ai.client.model.Conversation;
import org.e1c.edt.ai.client.model.ConversationID;
import org.e1c.edt.ai.client.model.Message;

/**
 * This interface is an API to 1C.AI service. It is used to create a realization of class,
 * which is aimed to provide connection
 * with the AI. It also provides access to manipulation: receiving messages,
 * conversations, sending feedback and getting their list for a dedicated chat.
 *
 * @author Bogdan Sushkov
 */
public interface IClientAI
{
    /**
     * Creates chat with given parameters from preferences page
     * @param settings
     * @return UUID of created chat or null if error while creation occured
     * @throws AIClientException
     */
    public ConversationID createChat() throws AIClientException;;

    /**
     * Get conversation via its UUID. If conversation doesn't exist, Internal server error will be returned
     * @param UUID
     * @return conversation
     * @throws AIClientException
     */
    public Conversation getConversation(String UUID) throws AIClientException;

    /**
     * Sends message to AI and updates chat state at moment when getting response.
     * @param message
     * @return array of messages from AI
     * @throws AIClientException
     */
    public ArrayList<Message> sendMessage(String message) throws AIClientException;
}
