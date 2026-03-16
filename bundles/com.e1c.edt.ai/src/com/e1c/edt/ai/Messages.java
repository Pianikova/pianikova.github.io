/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import org.eclipse.osgi.util.NLS;

/**
 * That class was created in order to provide message localization.
 *
 * @author Bogdan Sushkov
 */
public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "com.e1c.edt.ai.messages"; //$NON-NLS-1$
    public static String CodeCompletionPolicy_Off;
    public static String CodeCompletionPolicy_OffShortDescription;
    public static String CodeCompletionPolicy_OffDescription;
    public static String CodeCompletionPolicy_Manual;
    public static String CodeCompletionPolicy_ManualShortDescription;
    public static String CodeCompletionPolicy_ManualDescription;
    public static String CodeCompletionPolicy_Moderate;
    public static String CodeCompletionPolicy_ModerateShortDescription;
    public static String CodeCompletionPolicy_ModerateDescription;
    public static String CodeCompletionPolicy_Intensive;
    public static String CodeCompletionPolicy_IntensiveShortDescription;
    public static String CodeCompletionPolicy_IntensiveDescription;
    public static String FileMenu_CopyAbsolutePath;
    public static String FileMenu_CopyFileName;
    public static String FileMenu_CopyLink;
    public static String McpTools_RetryableError;
    public static String StatusOffline;
    public static String StatusOnline;
    public static String StatusMissingToken;
    public static String StatusServerError;
    public static String StatusSettingsChanged;
    public static String StatusSessionExpired;
    public static String StatusTokenFailed;
    public static String StatusSSLFailed;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
