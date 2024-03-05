/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client.model;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

/**
 * This class contains information about response-JSON fields. It uses
 * as Serialized-from-JSON object. It keeps response data after request
 * with getting certain conversation.
 * @author Bogdan Sushkov
 */
public class Conversation
{
    private Info info;
    private ArrayList<Message> messages;
    public Conversation() {
        messages = new ArrayList<>();
    }

    /**
     * Returns extra-info
     * @return the info
     */
    public Info getInfo()
    {
        return info;
    }

    /**
     * Sets extra-info
     * @param info the info to set
     */
    public void setInfo(Info info)
    {
        this.info = info;
    }

    /**
     * Returns array of messages in current conversation
     * @return the messages
     */
    public ArrayList<Message> getMessages()
    {
        return messages;
    }

    /**
     * Sets array of messages in current conversation
     * @param messages the messages to set
     */
    public void setMessages(ArrayList<Message> messages)
    {
        this.messages = messages;
    }

    /**
     * This class contains extra-information about conversation.
     * @author Bogdan Sushkov
     */
    public class Info
    {
        private ArrayList<Database> databases;
        @SerializedName("model_name")
        private String modelName;

        /**
         * @return the databases
         */
        public ArrayList<Database> getDatabases()
        {
            return databases;
        }

        /**
         * @param databases the databases to set
         */
        public void setDatabases(ArrayList<Database> databases)
        {
            this.databases = databases;
        }

        /**
         * @return the modelName
         */
        public String getModelName()
        {
            return modelName;
        }

        /**
         * @param modelName the modelName to set
         */
        public void setModelName(String modelName)
        {
            this.modelName = modelName;
        }
    }
}
