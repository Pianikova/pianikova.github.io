/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client;

import java.util.ArrayList;

/**
 * This class holds fields with parameters from AI preference page.
 * @author Bogdan Sushkov
 *
 */
public class AISettings
{
    private ArrayList<String> accessRoles;
    private ArrayList<String> tags;
    private String apiURL;
    private String chatURL;
    private String clientToken;
    private String modelName;
    private String dataBaseName;
    private String documentPath;

    /**
     * Constructs ChatStarter object
     * @param accessRoles
     * @param tags
     * @param chatURL
     * @param clientToken
     * @param modelName
     * @param dataBaseName
     */
    public AISettings(ArrayList<String> accessRoles, ArrayList<String> tags, String apiURL, String chatURL,
        String clientToken,
        String modelName, String dataBaseName, String documentPath)
    {
        this.accessRoles = accessRoles;
        this.tags = tags;
        this.apiURL = apiURL;
        this.chatURL = chatURL;
        this.clientToken = clientToken;
        this.modelName = modelName;
        this.dataBaseName = dataBaseName;
        this.documentPath = documentPath;
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
     * @return the apiURL
     */
    public String getApiURL()
    {
        return apiURL;
    }

    /**
     * @return the chatURL
     */
    public String getChatURL()
    {
        return chatURL;
    }

    /**
     * @return the clientToken
     */
    public String getClientToken()
    {
        return clientToken;
    }

    /**
     * @param clientToken the clientToken to set
     */
    public void setClientToken(String clientToken)
    {
        this.clientToken = clientToken;
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
