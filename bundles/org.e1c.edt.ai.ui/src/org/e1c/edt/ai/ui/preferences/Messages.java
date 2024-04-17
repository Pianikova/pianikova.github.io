/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import org.eclipse.osgi.util.NLS;

/**
 * That class was created in order to provide message localization.
 * @author Bogdan Sushkov
 *
 */
public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "org.e1c.edt.ai.ui.preferences.messages"; //$NON-NLS-1$
    public static String ClientAIPreferencePage_Service_parameters;
    public static String ClientAIPreferencePage_Api_URL;
    public static String ClientAIPreferencePage_Chat_URL;
    public static String ClientAIPreferencePage_Client_token;
    public static String ClientAIPreferencePage_Database_name;
    public static String ClientAIPreferencePage_AI_model;
    public static String ClientAIPreferencePage_Tags;
    public static String ClientAIPreferencePage_Access_roles;
    public static String ClientAIPreferencePage_Document_path;
    public static String ClientAIPreferencePage_LLL_parameters;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
