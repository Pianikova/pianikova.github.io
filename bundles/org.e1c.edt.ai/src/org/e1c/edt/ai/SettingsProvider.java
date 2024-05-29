/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.client.AISettings;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class SettingsProvider
    implements ISettingsProvider
{
    private final static String QUERY_TEMPLATE = "?client_id=%s&client_uid=%s"; //$NON-NLS-1$
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
        var clientToken = settingsStore.getString(ISettingsStore.CLIENT_TOKEN);
        var clientUID = settingsStore.getString(ISettingsStore.CLIENT_UID);
        var accessRoles =
            new ArrayList<>(Arrays.asList(settingsStore.getString(ISettingsStore.ACCESS_ROLES).split(","))); //$NON-NLS-1$
        var tags =
            new ArrayList<>(Arrays.asList(settingsStore.getString(ISettingsStore.TAGS).split(","))); //$NON-NLS-1$
        URL apiURL = null;
        URL chatURL = null;
        try
        {
            var rootURL = new URL(normalize(settingsStore.getString(ISettingsStore.APIURL)));
            apiURL = new URL(rootURL,
                normalize(rootURL.getFile())
                    + String.format(QUERY_TEMPLATE, URLEncoder.encode(clientToken, StandardCharsets.UTF_8),
                        URLEncoder.encode(clientUID, StandardCharsets.UTF_8)));
            chatURL = new URL(normalize(settingsStore.getString(ISettingsStore.CHATURL)));
        }
        catch (MalformedURLException e)
        {
            log.logError(e);
            return Optional.empty();
        }

        var modelName = settingsStore.getString(ISettingsStore.MODEL_NAME);
        var databaseName = settingsStore.getString(ISettingsStore.DATABASE_NAME);
        var docPath = settingsStore.getString(ISettingsStore.DOCUMENT_PATH);
        var llmParameters = settingsStore.getString(ISettingsStore.LLM_PARAMETERS);
        var _apiURL = apiURL;
        var _chatURL = chatURL;
        return parametersParser.parse(llmParameters)
            .map(params -> new AISettings(accessRoles, tags, _apiURL, _chatURL, clientToken, clientUID, modelName,
                databaseName,
                docPath, params));
    }

    private String normalize(String text)
    {
        if (text.isEmpty())
        {
            return text;
        }

        text = text.trim();

        if (text.endsWith("/")) //$NON-NLS-1$
        {
            text = text.substring(0, text.length() - 1);
        }

        return text;
    }
}
