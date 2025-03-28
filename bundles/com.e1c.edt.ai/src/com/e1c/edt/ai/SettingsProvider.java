/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.concurrent.ExecutionException;

import com.e1c.edt.ai.assistent.model.Parameters;
import com.e1c.edt.ai.client.AISettings;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

public class SettingsProvider
    implements ISettingsProvider
{
    private final ISettingsStore settingsStore;
    private final IParser<String, Parameters> parametersParser;
    private final IIdProvider idProvider;
    private final IDefaultSettings defaultSettings;
    private final Cache<String, Parameters> parametersCache =
        CacheBuilder.newBuilder().maximumSize(2).build();

    @Inject
    public SettingsProvider(ISettingsStore settingsStore, IParser<String, Parameters> parametersParser,
        IIdProvider idProvider, IDefaultSettings defaultSettings)
    {
        Preconditions.checkNotNull(settingsStore);
        Preconditions.checkNotNull(parametersParser);
        Preconditions.checkNotNull(idProvider);
        Preconditions.checkNotNull(defaultSettings);
        this.settingsStore = settingsStore;
        this.parametersParser = parametersParser;
        this.idProvider = idProvider;
        this.defaultSettings = defaultSettings;
    }

    @Override
    public synchronized AISettings getSettings()
    {
        var clientToken = settingsStore.getString(ISettingsStore.CLIENT_TOKEN).trim();
        if (clientToken != null)
        {
            clientToken = clientToken.trim();
        }

        var llmParameters = settingsStore.getString(ISettingsStore.PARAMETERS);
        Parameters parameters;
        try
        {
            parameters = parametersCache.get(llmParameters,
                () -> parametersParser.parse(llmParameters).orElseGet(() -> new Parameters(defaultSettings)));
        }
        catch (ExecutionException e)
        {
            parameters = new Parameters(defaultSettings);
        }

        var settings = new AISettings(clientToken, idProvider.getId(), parameters);
        return settings;
    }
}
