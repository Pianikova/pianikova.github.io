/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * This class usually used to create response with sending message to service.
 * @author Bogdan Sushkov
 *
 */
public class MessageRequest
{
    private String text;
    @SerializedName("parent_uuid")
    private String parentUUID;

    /**
     * Constructs object MessageRequest
     * @param text
     * @param parentUUID
     */
    public MessageRequest(String text, String parentUUID)
    {
        this.text = text;
        this.parentUUID = parentUUID;
    }

    /**
     * @return the text
     */
    public String getText()
    {
        return text;
    }
    /**
     * @param text the text to set
     */
    public void setText(String text)
    {
        this.text = text;
    }
    /**
     * @return the parentUUID
     */
    public String getParentUUID()
    {
        return parentUUID;
    }
    /**
     * @param parentUUID the parentUUID to set
     */
    public void setParentUUID(String parentUUID)
    {
        this.parentUUID = parentUUID;
    }

}
