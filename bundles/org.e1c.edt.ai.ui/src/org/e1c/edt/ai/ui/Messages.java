/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.osgi.util.NLS;

/**
 * That class was created in order to provide message localization.
 *
 * @author Bogdan Sushkov
 */
public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "org.e1c.edt.ai.ui.messages"; //$NON-NLS-1$
    public static String AIName;
    public static String CodeCompletionJobName;
    public static String ChatInteractionJobName;
    public static String FeedbackDialogBoxTitle;
    public static String FeedbackDialogTitle;
    public static String FeedbackDialogMessage;
    public static String FeedbackDialogRefersToCodeCompletion;
    public static String FeedbackDialogIssueType;
    public static String FeedbackDialogDescription;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
