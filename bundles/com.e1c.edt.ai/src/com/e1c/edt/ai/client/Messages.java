/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.client;

import org.eclipse.osgi.util.NLS;

/**
 * That class was created in order to provide message localization.
 *
 * @author Bogdan Sushkov
 */
public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "com.e1c.edt.ai.client.messages"; //$NON-NLS-1$
    public static String ClientAI_Cannot_connect;
    public static String ClientAI_Response_error;
    public static String ClientAI_Server_status_500;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
