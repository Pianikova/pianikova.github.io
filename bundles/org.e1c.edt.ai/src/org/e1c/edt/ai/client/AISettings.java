/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client;

import java.net.URL;

import org.e1c.edt.ai.assistent.model.Parameters;

/**
 * This class holds fields with parameters from AI preference page.
 * @author Bogdan Sushkov
 *
 */
public class AISettings
{
    private final URL apiURL;
    private final String clientToken;
    private final String clientUniqueId;
    private final Parameters llmParameters;

    public AISettings(URL apiURL, String clientToken, String clientUniqueId, Parameters llmParameters)
    {
        this.apiURL = apiURL;
        this.clientToken = clientToken;
        this.clientUniqueId = clientUniqueId;
        this.llmParameters = llmParameters;
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

    public Parameters getLlmParameters()
    {
        return llmParameters;
    }
}
