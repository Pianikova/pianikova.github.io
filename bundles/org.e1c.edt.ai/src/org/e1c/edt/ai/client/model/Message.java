/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client.model;

import org.e1c.edt.ai.client.model.internal.MessageContent;

import com.google.gson.annotations.SerializedName;

/**
 * This class contains information about response-JSON fields. It uses
 * as Serialized-from-JSON object. It keeps response data after request
 * with sending message in chat.
 * @author Bogdan Sushkov
 *
 */
public class Message
{
    private String uuid;
    private String role;
    private MessageContent content;
    @SerializedName("parent_uuid")
    private String parentUUID;
    @SerializedName("create_time")
    private String createTime;

    /**
     * Returns <code>UUID</code> parameter.
     * @return the uuid
     */
    public String getUUID()
    {
        return uuid;
    }

    /**
     * Sets <code>UUID</code> parameter.
     * @param uuid the uuid to set
     */
    public void setUUID(String uuid)
    {
        this.uuid = uuid;
    }

    /**
     * Returns <code>role</code> parameter.
     * @return the role
     */
    public String getRole()
    {
        return role;
    }

    /**
     * Sets <code>role</code> parameter.
     * @param role the role to set
     */
    public void setRole(String role)
    {
        this.role = role;
    }

    /**
     * Returns <code>content</code> parameter.
     * @return the content
     */
    public MessageContent getContent()
    {
        return content;
    }

    /**
     * Sets <code>content</code> parameter.
     * @param content the content to set
     */
    public void setContent(MessageContent content)
    {
        this.content = content;
    }

    /**
     * Returns <code>parent_uuid</code> parameter.
     * @return the parent_uuid
     */
    public String getParentUuid()
    {
        return parentUUID;
    }

    /**
     * Sets <code>parent_uuid</code> parameter.
     * @param parent_uuid the parent_uuid to set
     */
    public void setParentUuid(String parent_uuid)
    {
        this.parentUUID = parent_uuid;
    }

    /**
     * Returns <code>create_time</code> parameter.
     * @return the create_time
     */
    public String getCreateTime()
    {
        return createTime;
    }

    /**
     * Sets <code>create_time</code> parameter.
     * @param create_time the create_time to set
     */
    public void setCreateTime(String create_time)
    {
        this.createTime = create_time;
    }
}
