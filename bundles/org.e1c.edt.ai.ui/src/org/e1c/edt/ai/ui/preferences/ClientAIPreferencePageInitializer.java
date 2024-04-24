/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import org.e1c.edt.ai.ISettingsStore;
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
        store.setDefault(ISettingsStore.APIURL, "https://coder.1c.ai/api/v1/generate"); //$NON-NLS-1$
        store.setDefault(ISettingsStore.CHATURL, "http://10.70.5.31:4000/"); //$NON-NLS-1$
        store.setDefault(ISettingsStore.CLIENTTOKEN, "user_test"); //$NON-NLS-1$
        store.setDefault(ISettingsStore.DATABASENAME, "edt_doc"); //$NON-NLS-1$
        store.setDefault(ISettingsStore.MODELNAME, "openchat_7b"); //$NON-NLS-1$
        store.setDefault(ISettingsStore.LLMPARAMETERS,
            "max_new_tokens=120; temperature=0.2; top_p=0.95; top_k=10"); //$NON-NLS-1$
        store.setDefault(ISettingsStore.MAXASSISTANTTEXTSIZE, ISettingsStore.DEFAULTMAXASSISTANTTEXTSIZE);
    }
}
