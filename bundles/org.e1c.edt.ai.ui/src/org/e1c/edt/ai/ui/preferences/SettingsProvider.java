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
        ArrayList<String> accessRoles =
            new ArrayList<>(Arrays.asList(preferenceStore.getString(ClientAIPreferencePage.ACCESSROLES).split(","))); //$NON-NLS-1$
        ArrayList<String> tags =
            new ArrayList<>(Arrays.asList(preferenceStore.getString(ClientAIPreferencePage.TAGS).split(","))); //$NON-NLS-1$
        String apiURL = preferenceStore.getString(ClientAIPreferencePage.APIURL);
        String chatURL = preferenceStore.getString(ClientAIPreferencePage.CHATURL);
        String token = preferenceStore.getString(ClientAIPreferencePage.CLIENTTOKEN);
        String modelName = preferenceStore.getString(ClientAIPreferencePage.MODELNAME);
        String databaseName = preferenceStore.getString(ClientAIPreferencePage.DATABASENAME);
        String docPath = preferenceStore.getString(ClientAIPreferencePage.DOCUMENTPATH);
        String llmParameters = preferenceStore.getString(ClientAIPreferencePage.LLMPARAMETERS);
        return new AISettings(accessRoles, tags, apiURL, chatURL, token, modelName, databaseName, docPath,
            parametersParser.parse(llmParameters));
    }
}
