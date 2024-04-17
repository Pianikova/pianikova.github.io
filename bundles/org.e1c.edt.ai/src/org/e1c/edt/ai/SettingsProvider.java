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

import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.client.AISettings;

public class SettingsProvider
    implements ISettingsProvider
{
    private ILog log;
    private ISettingsStore settingsStore;
    private IParser<String, Parameters> parametersParser;

    public SettingsProvider(ILog log, ISettingsStore settingsStore, IParser<String, Parameters> parametersParser)
    {
        this.log = log;
        this.settingsStore = settingsStore;
        this.parametersParser = parametersParser;
    }

    @Override
    public AISettings getSettings()
    {
        var clientToken = settingsStore.getString(ISettingsStore.CLIENTTOKEN);
        var accessRoles =
            new ArrayList<>(Arrays.asList(settingsStore.getString(ISettingsStore.ACCESSROLES).split(","))); //$NON-NLS-1$
        var tags =
            new ArrayList<>(Arrays.asList(settingsStore.getString(ISettingsStore.TAGS).split(","))); //$NON-NLS-1$
        URL apiURL = null;
        URL chatURL = null;
        try
        {
            var rootURL = new URL(settingsStore.getString(ISettingsStore.APIURL));
            apiURL = new URL(rootURL, "/generate?client_id=" + URLEncoder.encode(clientToken, StandardCharsets.UTF_8)); //$NON-NLS-1$
            chatURL = new URL(settingsStore.getString(ISettingsStore.CHATURL));
        }
        catch (MalformedURLException e)
        {
            log.logError(e);
        }

        var modelName = settingsStore.getString(ISettingsStore.MODELNAME);
        var databaseName = settingsStore.getString(ISettingsStore.DATABASENAME);
        var docPath = settingsStore.getString(ISettingsStore.DOCUMENTPATH);
        var llmParameters = settingsStore.getString(ISettingsStore.LLMPARAMETERS);
        var maxAssistantTextSize = settingsStore.getInt(ISettingsStore.MAXASSISTANTTEXTSIZE);
        if (maxAssistantTextSize <= 0)
        {
            maxAssistantTextSize = ISettingsStore.DefaultMaxAssistantTextSize;
        }

        return new AISettings(accessRoles, tags, apiURL, chatURL, clientToken, modelName, databaseName, docPath,
            parametersParser.parse(llmParameters), maxAssistantTextSize);
    }
}
