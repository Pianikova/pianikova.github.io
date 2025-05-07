/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.preferences;

import org.eclipse.osgi.util.NLS;

/**
 * That class was created in order to provide message localization.
 * @author Bogdan Sushkov
 *
 */
public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "com.e1c.edt.ai.ui.preferences.messages"; //$NON-NLS-1$
    public static String ClientAIPreferencePage_Api_URL;
    public static String ClientAIPreferencePage_Client_Token;
    public static String ClientAIPreferencePage_CodeCompletionLinesCount;
    public static String ClientAIPreferencePage_Language;
    public static String ClientAIPreferencePage_Parameters;

    public static String ClientAIPreferencePage_Language_Default;
    public static String ClientAIPreferencePage_Language_English;
    public static String ClientAIPreferencePage_Language_Russian;

    public static String ClientAIPreferencePage_CodeCompletionPolicy;

    public static String Error_UnableToParse;
    public static String Error_OutOfRange;
    public static String Error_Unknown;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
