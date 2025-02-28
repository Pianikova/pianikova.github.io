/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.osgi.util.NLS;

/**
 * That class was created in order to provide message localization.
 *
 * @author Bogdan Sushkov
 */
public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "com.e1c.edt.ai.ui.messages"; //$NON-NLS-1$
    public static String AIName;
    public static String CodeCompletionJobName;
    public static String CodeCompletionBackgroundJobName;
    public static String CodeCompletionBackgroundScanSubtaskName;
    public static String CodeCompletionBackgroundHashSubtaskName;
    public static String CodeCompletionBackgroundSyncSubtaskName;
    public static String ChatInteractionJobName;
    public static String FeedbackDialogBoxTitle;
    public static String FeedbackDialogTitle;
    public static String FeedbackDialogMessage;
    public static String FeedbackDialogRefersToCodeCompletion;
    public static String FeedbackDialogIssueType;
    public static String FeedbackDialogDescription;
    public static String ReplaceCode;
    public static String FixCodeRequestDetails;
    public static String FixCodeDefaultDetails;
    public static String StatusOffline;
    public static String StatusOnline;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
