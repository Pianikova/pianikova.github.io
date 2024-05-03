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
    private final ArrayList<String> accessRoles;
    private final ArrayList<String> tags;
    private final URL apiURL;
    private final URL chatURL;
    private final String clientToken;
    private final String clientUniqueId;
    private final String modelName;
    private final String dataBaseName;
    private final String documentPath;
    private final Parameters llmParameters;
    private final int maxAssistantTextSize;

    public AISettings(ArrayList<String> accessRoles, ArrayList<String> tags, URL apiURL, URL chatURL,
        String clientToken, String clientUniqueId,
        String modelName, String dataBaseName, String documentPath, Parameters llmParameters, int maxAssistantTextSize)
    {
        this.accessRoles = accessRoles;
        this.tags = tags;
        this.apiURL = apiURL;
        this.chatURL = chatURL;
        this.clientToken = clientToken;
        this.clientUniqueId = clientUniqueId;
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

    public ArrayList<String> getTags()
    {
        return tags;
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

    public String getClientUniqueId()
    {
        return clientUniqueId;
    }

    public String getModelName()
    {
        return modelName;
    }

    public String getDataBaseName()
    {
        return dataBaseName;
    }

    public String getDocumentPath()
    {
        return documentPath;
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
