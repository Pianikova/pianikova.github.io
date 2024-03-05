/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client.model;

import org.e1c.edt.ai.client.model.internal.FeedbackContent;

import com.google.gson.annotations.SerializedName;

/**
 * This class keeps feedback data in correct JSON-format. It usually uses
 * to form request with new feedback on current AI-message.
 * @author Bogdan Sushkov
 */
public class FeedbackRequest
{
    @SerializedName("message_uuid")
    private final String messageUUID;
    @SerializedName("client_id")
    private final String clientID;
    private final FeedbackContent content;
    @SerializedName("create_time")
    private final String createTime;

    private FeedbackRequest(FeedbackRequestBuilder builder)
    {
        this.messageUUID = builder.builderMessageUUID;
        this.clientID = builder.builderClientID;
        this.content = builder.builderContent;
        this.createTime = builder.builderCreateTime;
    }

    /**
     * Returns <code>messageUUID</code> parameter.
     * @return the message_uuid
     */
    public String getMessageUUID()
    {
        return messageUUID;
    }

    /**
     * Returns <code>clientID</code> parameter.
     * @return the client_id
     */
    public String getClientID()
    {
        return clientID;
    }

    /**
     * Returns <code>content</code>, which contains feedback
     * parameters.
     * @return the content
     */
    public FeedbackContent getContent()
    {
        return content;
    }

    /**
     * Returns <code>createTime</code> parameter.
     * @return the create_time
     */
    public String getCreateTime()
    {
        return createTime;
    }

    /**
     * This is builder-class, which can be used to create FeedbackRequest.
     * @author Bogdan Sushkov
     */
    public static class FeedbackRequestBuilder
    {
        private String builderMessageUUID;
        private String builderClientID;
        private FeedbackContent builderContent;
        private String builderCreateTime;

        /**
         * Sets <code>messageUUID</code> parameter.
         * @param messageUUID
         * @return builder
         */
        public FeedbackRequestBuilder setMessageUUID(String messageUUID)
        {
            this.builderMessageUUID = messageUUID;
            return this;
        }

        /**
         * Sets <code>clientID</code> parameter.
         * @param clientID
         * @return builder
         */
        public FeedbackRequestBuilder setClientID(String clientID)
        {
            this.builderClientID = clientID;
            return this;
        }

        /**
         * Sets <code>content</code> parameter.
         * @param softScore
         * @param hardScore
         * @return builder
         */
        public FeedbackRequestBuilder setContent(int softScore, int hardScore)
        {
            this.builderContent = new FeedbackContent(softScore, hardScore);
            return this;
        }

        /**
         * Sets <code>time</code> of creation feedback parameter.
         * @param time
         * @return builder
         */
        public FeedbackRequestBuilder setCreateTime(String time)
        {
            this.builderCreateTime = time;
            return this;
        }

        /**
         * Builds <code>FeedbackRequest</code> object with given parameters.
         * @return created <code>FeedbackRequest</code> object.
         */
        public FeedbackRequest build()
        {
            return new FeedbackRequest(this);
        }
    }
}
