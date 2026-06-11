/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

import org.eclipse.osgi.util.NLS;

/**
 * Localized labels for the Workmate navigator nodes.
 */
public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "com.e1c.edt.ai.ui.navigator.messages"; //$NON-NLS-1$

    public static String node_workmate;
    public static String node_user;
    public static String node_workspace;
    public static String node_project;
    public static String node_skills;
    public static String badge_overridden;
    public static String action_openCreate;
    public static String action_open;
    public static String action_reset;
    public static String hint_useChat;

    static
    {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
