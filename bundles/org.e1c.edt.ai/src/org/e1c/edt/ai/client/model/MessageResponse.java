/*
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.client.model;

import org.e1c.edt.ai.client.model.internal.MessageContent;

import com.google.gson.annotations.SerializedName;

/**
 * This class keeps data of message. It usually uses
 * to form response after sending new message to chat.
 * @author Bogdan Sushkov
 */
public class MessageResponse
{
    private String uuid;
    private String role;
    @SerializedName("message_type")
    private String messageType;
    private MessageContent content;
    @SerializedName("parent_uuid")
    private String parentUUID;

//    public MessageResponse(MessageRequestBuilder requestBody)
//    {
//        this.conversationUUID = requestBody.conversationUUID;
//        this.role = requestBody.role;
//        this.messageType = requestBody.messageType;
//        this.content = requestBody.content;
//        this.parentUUID = requestBody.parentUUID;
//    }

    /**
     * Returns <code>uuid</code> parameter.
     * @return uuid
     */
    public String getUUID()
    {
        return this.uuid;
    }

    /**
     * Returns <code>role</code> parameter.
     * @return role
     */
    public String getRole()
    {
        return this.role;
    }

    /**
     * Returns <code>message_type</code> parameter.
     * @return message_type
     */
    public String getMessageType()
    {
        return this.messageType;
    }

    /**
     * Returns <code>content</code> parameter.
     * @return content
     */
    public MessageContent getContent()
    {
        return this.content;
    }

    /**
     * Returns <code>parent_uuid</code> parameter.
     * @return parent_uuid
     */
    public String getParentUUID()
    {
        return this.parentUUID;
    }

//    /**
//     * This is builder-class, which can be used to create MessageRequest.
//     * @author Bogdan Sushkov
//     */
//    public static class MessageRequestBuilder
//    {
//        private String conversationUUID;
//        private String role = "user"; //$NON-NLS-1$
//        private String messageType = "user_message"; //$NON-NLS-1$
//        private MessageContent content;
//        private String parentUUID;
//
//        /**
//         * Sets <code>conversationUUID</code> parameter.
//         * @param UUID
//         * @return builder
//         */
//        public MessageRequestBuilder setConversationUUID(String UUID)
//        {
//            this.conversationUUID = UUID;
//            return this;
//        }
//
//        /**
//         * Sets <code>role</code> parameter.
//         * @param role
//         * @return builder
//         */
//        public MessageRequestBuilder setRole(String role)
//        {
//            this.role = role;
//            return this;
//        }
//
//        /**
//         * Sets <code>MessageType</code> parameter.
//         * @param messageType
//         * @return builder
//         */
//        public MessageRequestBuilder setMessageType(String messageType)
//        {
//            this.messageType = messageType;
//            return this;
//        }
//
//        /**
//         * Sets <code>Content</code> parameter.
//         * @param content
//         * @return builder
//         */
//        public MessageRequestBuilder setContent(MessageContent content)
//        {
//            this.content = content;
//            return this;
//        }
//
//        /**
//         * Sets <code>parentUUID</code> parameter.
//         * @param UUID
//         * @return builder
//         */
//        public MessageRequestBuilder setParentUUID(String UUID)
//        {
//            this.parentUUID = UUID;
//            if (this.parentUUID == null)
//            {
//                this.parentUUID = new String();
//            }
//            return this;
//        }
//
//        /**
//         * Builds <code>MessageRequest</code> object with given parameters.
//         * @return created <code>MessageRequest</code> object.
//         */
//        public MessageResponse build()
//        {
//            return new MessageResponse(this);
//        }
//    }
}
