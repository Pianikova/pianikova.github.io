/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.client.AISettings;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class SettingsProvider
    implements ISettingsProvider
{
    private final ILog log;
    private final ISettingsStore settingsStore;
    private final IParser<String, Parameters> parametersParser;

    @Inject
    public SettingsProvider(ILog log, ISettingsStore settingsStore, IParser<String, Parameters> parametersParser)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settingsStore);
        Preconditions.checkNotNull(parametersParser);
        this.log = log;
        this.settingsStore = settingsStore;
        this.parametersParser = parametersParser;
    }

    @Override
    public Optional<AISettings> getSettings()
    {
        var clientToken = settingsStore.getString(ISettingsStore.CLIENT_TOKEN).trim();
        if (clientToken != null)
        {
            clientToken = clientToken.trim();
        }

        var clientUID = settingsStore.getString(ISettingsStore.CLIENT_UID);
        if (clientUID != null)
        {
            clientUID = clientUID.trim();
        }

        URL apiURL = null;
        try
        {
            apiURL = new URL(normalize(settingsStore.getString(ISettingsStore.APIURL)));
        }
        catch (MalformedURLException e)
        {
            log.logError(e);
            return Optional.empty();
        }

        var llmParameters = settingsStore.getString(ISettingsStore.LLM_PARAMETERS);
        var parameters = parametersParser.parse(llmParameters).orElseGet(() -> new Parameters());
        var settings = new AISettings(apiURL, clientToken, clientUID, parameters);
        return Optional.of(settings);
    }

    private String normalize(String text)
    {
        if (text.isEmpty())
        {
            return text;
        }

        text = text.trim();
        if (!text.endsWith("/")) //$NON-NLS-1$
        {
            text = text + "/"; //$NON-NLS-1$
        }

        return text;
    }
}
