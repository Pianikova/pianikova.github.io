/*
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.client;

import java.util.Objects;

import org.e1c.edt.ai.assistent.model.Parameters;

/**
 * This class holds fields with parameters from AI preference page.
 * @author Bogdan Sushkov
 *
 */
public class AISettings
{
    private final String clientToken;
    private final String clientUniqueId;
    private final Parameters llmParameters;

    public AISettings(String clientToken, String clientUniqueId, Parameters llmParameters)
    {
        this.clientToken = clientToken;
        this.clientUniqueId = clientUniqueId;
        this.llmParameters = llmParameters;
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

    @Override
    public int hashCode()
    {
        return Objects.hash(clientToken, clientUniqueId, llmParameters);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AISettings other = (AISettings)obj;
        return Objects.equals(clientToken, other.clientToken)
            && Objects.equals(clientUniqueId, other.clientUniqueId)
            && Objects.equals(llmParameters, other.llmParameters);
    }
}
