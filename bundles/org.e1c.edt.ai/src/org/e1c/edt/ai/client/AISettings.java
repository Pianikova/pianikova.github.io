/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client;

import java.net.URL;
import java.util.List;

import org.e1c.edt.ai.assistent.model.Parameters;

/**
 * This class holds fields with parameters from AI preference page.
 * @author Bogdan Sushkov
 *
 */
public class AISettings
{
    private final List<String> accessRoles;
    private final List<String> tags;
    private final URL apiURL;
    private final String clientToken;
    private final String clientUniqueId;
    private final String modelName;
    private final String dataBaseName;
    private final String documentPath;
    private final Parameters llmParameters;

    public AISettings(List<String> accessRoles, List<String> tags, URL apiURL, String clientToken,
        String clientUniqueId, String modelName, String dataBaseName, String documentPath, Parameters llmParameters)
    {
        this.accessRoles = accessRoles;
        this.tags = tags;
        this.apiURL = apiURL;
        this.clientToken = clientToken;
        this.clientUniqueId = clientUniqueId;
        this.modelName = modelName;
        this.dataBaseName = dataBaseName;
        this.documentPath = documentPath;
        this.llmParameters = llmParameters;
    }

    public List<String> getAccessRoles()
    {
        return accessRoles;
    }

    public List<String> getTags()
    {
        return tags;
    }

    public URL getApiURL()
    {
        return apiURL;
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
}
