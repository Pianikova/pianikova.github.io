/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import org.e1c.edt.ai.ui.Activator;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This class contains default parameters on the preferences page with the
 * AI chat settings. Default parameters allows user to test standart version of the AI.
 * @author Bogdan Sushkov
 *
 */
public class ClientAIPreferencePageInitializer
    extends AbstractPreferenceInitializer
{
    @Override
    public void initializeDefaultPreferences()
    {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(ClientAIPreferencePage.SERVICEURL, "http://llm-api2.ailab:8889/api"); //$NON-NLS-1$
        store.setDefault(ClientAIPreferencePage.CLIENTTOKEN, "user_test"); //$NON-NLS-1$
        store.setDefault(ClientAIPreferencePage.DATABASENAME, "edt_doc"); //$NON-NLS-1$
        store.setDefault(ClientAIPreferencePage.MODELNAME, "openchat_7b"); //$NON-NLS-1$
    }
}
