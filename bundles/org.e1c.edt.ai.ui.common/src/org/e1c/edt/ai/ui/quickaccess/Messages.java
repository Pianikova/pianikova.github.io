/*
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui.quickaccess;

import org.eclipse.osgi.util.NLS;

/**
 * That class was created in order to provide message localization.
 *
 * @author Bogdan Sushkov
 */
public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "org.e1c.edt.ai.ui.quickaccess.messages"; //$NON-NLS-1$
    public static String QuickAccessElementAskAI_0;
    public static String QuickAccessElementAskAI_1;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
