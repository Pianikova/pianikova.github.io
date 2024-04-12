/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;

import org.e1c.edt.ai.client.AISettings;
import org.e1c.edt.ai.ui.Activator;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This class allows user to get parameters from preference page.
 * @author Bogdan Sushkov
 *
 */
public class ClientAIPreferenceService
{
    private static IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();

    /**
     * This function create ChatStarter object with provided current user's preferences.
     * It can be used while creating chat with options located in AI preference page.
     * The settings are current at the time of receipt. Changing the settings will not
     * affect the created object in any way.
     *
     * @return ChatStarter object
     */
    public static AISettings getSettings()
    {
        ArrayList<String> accessRoles = new ArrayList<>(Arrays.asList(getPreferenceAccessRoles().split(","))); //$NON-NLS-1$
        ArrayList<String> tags = new ArrayList<>(Arrays.asList(getPreferenceTags().split(","))); //$NON-NLS-1$
        String apiURL = getPreferenceApiURL();
        String chatURL = getPreferenceChatURL();
        String token = getPreferenceClientToken();
        String modelName = getPreferenceModelName();
        String databaseName = getPreferenceDatabaseName();
        String docPath = getPreferenceDocumentPath();
        return new AISettings(accessRoles, tags, apiURL, chatURL, token, modelName, databaseName, docPath);
    }

    /*
     * Returns <code>serviceURL</code> parameter.
     * @return serviceURL
     */
    private static String getPreferenceApiURL()
    {
        return preferenceStore.getString(ClientAIPreferencePage.APIURL);
    }

    /*
     * Returns <code>serviceURL</code> parameter.
     * @return serviceURL
     */
    private static String getPreferenceChatURL()
    {
        return preferenceStore.getString(ClientAIPreferencePage.CHATURL);
    }

    /*
     * Returns <code>clientToken</code> parameter.
     * @return clientToken
     */
    private static String getPreferenceClientToken()
    {
        return preferenceStore.getString(ClientAIPreferencePage.CLIENTTOKEN);
    }

    /*
     * Returns <code>databaseName</code> parameter.
     * @return databaseName
     */
    private static String getPreferenceDatabaseName()
    {
        return preferenceStore.getString(ClientAIPreferencePage.DATABASENAME);
    }

    /*
     * Returns <code>modelName</code> parameter.
     * @return modelName
     */
    private static String getPreferenceModelName()
    {
        return preferenceStore.getString(ClientAIPreferencePage.MODELNAME);
    }

    /*
     * Returns <code>tags</code> parameter.
     * @return tags
     */
    private static String getPreferenceTags()
    {
        return preferenceStore.getString(ClientAIPreferencePage.TAGS);
    }

    /*
     * Returns <code>accessRoles</code> parameter.
     * @return accessRoles
     */
    private static String getPreferenceAccessRoles()
    {
        return preferenceStore.getString(ClientAIPreferencePage.ACCESSROLES);
    }

    /*
     * Returns <code>serviceURL</code> parameter.
     * @return serviceURL
     */
    private static String getPreferenceDocumentPath()
    {
        return preferenceStore.getString(ClientAIPreferencePage.DOCUMENTPATH);
    }
}
