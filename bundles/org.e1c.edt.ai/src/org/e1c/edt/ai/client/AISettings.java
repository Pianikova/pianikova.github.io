/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client;

import java.net.URL;
import java.util.ArrayList;

import org.e1c.edt.ai.assistent.model.Parameters;

/**
 * This class holds fields with parameters from AI preference page.
 * @author Bogdan Sushkov
 *
 */
public class AISettings
{
    private ArrayList<String> accessRoles;
    private ArrayList<String> tags;
    private URL apiURL;
    private URL chatURL;
    private String clientToken;
    private String modelName;
    private String dataBaseName;
    private String documentPath;
    private Parameters llmParameters;
    private int maxAssistantTextSize;

    public AISettings(ArrayList<String> accessRoles, ArrayList<String> tags, URL apiURL, URL chatURL,
        String clientToken,
        String modelName, String dataBaseName, String documentPath, Parameters llmParameters, int maxAssistantTextSize)
    {
        this.accessRoles = accessRoles;
        this.tags = tags;
        this.apiURL = apiURL;
        this.chatURL = chatURL;
        this.clientToken = clientToken;
        this.modelName = modelName;
        this.dataBaseName = dataBaseName;
        this.documentPath = documentPath;
        this.llmParameters = llmParameters;
        this.maxAssistantTextSize = maxAssistantTextSize;
    }

    public ArrayList<String> getAccessRoles()
    {
        return accessRoles;
    }

    public void setAccessRoles(ArrayList<String> accessRoles)
    {
        this.accessRoles = accessRoles;
    }

    public ArrayList<String> getTags()
    {
        return tags;
    }

    public void setTags(ArrayList<String> tags)
    {
        this.tags = tags;
    }

    public URL getApiURL()
    {
        return apiURL;
    }

    public URL getChatURL()
    {
        return chatURL;
    }

    public String getClientToken()
    {
        return clientToken;
    }

    public void setClientToken(String clientToken)
    {
        this.clientToken = clientToken;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getDataBaseName()
    {
        return dataBaseName;
    }

    public void setDataBaseName(String dataBaseName)
    {
        this.dataBaseName = dataBaseName;
    }

    public String getDocumentPath()
    {
        return documentPath;
    }

    public void setDocumentPath(String documentPath)
    {
        this.documentPath = documentPath;
    }

    public Parameters getLlmParameters()
    {
        return llmParameters;
    }

    public int getMaxAssistantTextSize()
    {
        return maxAssistantTextSize;
    }
}
