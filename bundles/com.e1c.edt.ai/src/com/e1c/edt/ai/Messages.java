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
    public static String CodeCompletionPolicy_OffDescription;
    public static String CodeCompletionPolicy_Focusing;
    public static String CodeCompletionPolicy_FocusingDescription;
    public static String CodeCompletionPolicy_Balance;
    public static String CodeCompletionPolicy_BalanceDescription;
    public static String CodeCompletionPolicy_Creativity;
    public static String CodeCompletionPolicy_CreativityDescription;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
