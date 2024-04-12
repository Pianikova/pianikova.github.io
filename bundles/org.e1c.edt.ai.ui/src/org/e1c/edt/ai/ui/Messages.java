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
    public static String AI_Prefix;
    public static String AI_Suggestions;
    public static String AI_Thinking;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
