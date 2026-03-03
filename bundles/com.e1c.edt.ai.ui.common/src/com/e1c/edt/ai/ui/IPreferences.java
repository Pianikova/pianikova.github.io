/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

/**
 * Service for opening preference dialogs in the IDE.
 */
public interface IPreferences
{
    /**
     * Preference page ID for AI settings.
     */
    String AI = "com.e1c.edt.ai.ui.clientPrefs"; //$NON-NLS-1$

    /**
     * Opens the preferences dialog at the specified page ID.
     *
     * @param pageId the ID of the preference page to display
     */
    void show(String pageId);
}
