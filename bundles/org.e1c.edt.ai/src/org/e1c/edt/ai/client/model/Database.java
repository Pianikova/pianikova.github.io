/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client.model;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

/**
 * This class contains additional information about the conversation.
 * It is usually used when users want to get certain conversation.
 * @author Bogdan Sushkov
 *
 */
public class Database
{
    @SerializedName("database_name")
    private String dataBaseName;
    @SerializedName("access_roles")
    private ArrayList<String> accessRoles;
    private ArrayList<String> tags;
    @SerializedName("document_path")
    private String documentPath;

    /**
     * Constructs object Database with given parameters.
     * @param dataBaseName
     * @param accessRoles
     * @param tags
     * @param documentPath
     */
    public Database(String dataBaseName, ArrayList<String> accessRoles, ArrayList<String> tags, String documentPath)
    {
        this.dataBaseName = dataBaseName;
        this.accessRoles = accessRoles;
        this.tags = tags;
        this.documentPath = documentPath;
    }

    /**
     * @return the dataBaseName
     */
    public String getDataBaseName()
    {
        return dataBaseName;
    }

    /**
     * @param dataBaseName the dataBaseName to set
     */
    public void setDataBaseName(String dataBaseName)
    {
        this.dataBaseName = dataBaseName;
    }

    /**
     * @return the accessRoles
     */
    public ArrayList<String> getAccessRoles()
    {
        return accessRoles;
    }

    /**
     * @param accessRoles the accessRoles to set
     */
    public void setAccessRoles(ArrayList<String> accessRoles)
    {
        this.accessRoles = accessRoles;
    }

    /**
     * @return the tags
     */
    public ArrayList<String> getTags()
    {
        return tags;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(ArrayList<String> tags)
    {
        this.tags = tags;
    }

    /**
     * @return the documentPath
     */
    public String getDocumentPath()
    {
        return documentPath;
    }

    /**
     * @param documentPath the documentPath to set
     */
    public void setDocumentPath(String documentPath)
    {
        this.documentPath = documentPath;
    }

}

