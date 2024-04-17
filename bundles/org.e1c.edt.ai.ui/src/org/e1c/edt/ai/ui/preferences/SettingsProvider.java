/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;

import org.e1c.edt.ai.IParser;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.client.AISettings;
import org.eclipse.jface.preference.IPreferenceStore;

public class SettingsProvider
    implements ISettingsProvider
{
    public static final int DefaultMaxAssistantTextSize = 1500;
    private IPreferenceStore preferenceStore;
    private IParser<String, Parameters> parametersParser;

    public SettingsProvider(IPreferenceStore preferenceStore, IParser<String, Parameters> parametersParser)
    {
        this.preferenceStore = preferenceStore;
        this.parametersParser = parametersParser;
    }

    @Override
    public AISettings getSettings()
    {
        var accessRoles =
            new ArrayList<>(Arrays.asList(preferenceStore.getString(ClientAIPreferencePage.ACCESSROLES).split(","))); //$NON-NLS-1$
        var tags =
            new ArrayList<>(Arrays.asList(preferenceStore.getString(ClientAIPreferencePage.TAGS).split(","))); //$NON-NLS-1$
        var apiURL = preferenceStore.getString(ClientAIPreferencePage.APIURL);
        var chatURL = preferenceStore.getString(ClientAIPreferencePage.CHATURL);
        var token = preferenceStore.getString(ClientAIPreferencePage.CLIENTTOKEN);
        var modelName = preferenceStore.getString(ClientAIPreferencePage.MODELNAME);
        var databaseName = preferenceStore.getString(ClientAIPreferencePage.DATABASENAME);
        var docPath = preferenceStore.getString(ClientAIPreferencePage.DOCUMENTPATH);
        var llmParameters = preferenceStore.getString(ClientAIPreferencePage.LLMPARAMETERS);
        var maxAssistantTextSize = preferenceStore.getInt(ClientAIPreferencePage.MAXASSISTANTTEXTSIZE);
        return new AISettings(accessRoles, tags, apiURL, chatURL, token, modelName, databaseName, docPath,
            parametersParser.parse(llmParameters), maxAssistantTextSize);
    }
}
