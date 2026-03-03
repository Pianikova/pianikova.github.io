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
    public static String Error_InvalidToken;
    public static String ClientAIPreferencePage_Client_Token_Tooltip;
    public static String TokenFieldEditor_Validate;
    public static String TokenFieldEditor_ValidationError;
    public static String TokenFieldEditor_ValidationSuccess;
    public static String TokenFieldEditor_TokenValid;
    public static String TokenFieldEditor_TokenInvalid;
    public static String ClientAIPreferencePage_CodeCompletionPolicy_Tooltip;
    public static String ClientAIPreferencePage_CodeCompletionLinesCount_Tooltip;
    public static String ClientAIPreferencePage_Language_Tooltip;
    public static String ClientAIPreferencePage_Parameters_Tooltip;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
