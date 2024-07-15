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
        store.setDefault(ISettingsStore.APIURL, "https://llms.1c.ai/code/api/v1/"); //$NON-NLS-1$
        store.setDefault(ISettingsStore.CHATURL, "http://10.70.2.171:4000/"); //$NON-NLS-1$
        store.setDefault(ISettingsStore.CLIENT_TOKEN, "user_test"); //$NON-NLS-1$
        store.setDefault(ISettingsStore.DATABASE_NAME, "edt_doc"); //$NON-NLS-1$
        store.setDefault(ISettingsStore.MODEL_NAME, "openchat_7b"); //$NON-NLS-1$
        store.setDefault(ISettingsStore.LLM_PARAMETERS, ""); //$NON-NLS-1$
        store.setDefault(ISettingsStore.MAX_ASSISTANT_TEXT_SIZE, ISettingsStore.DEFAULT_MAX_ASSISTANT_TEXT_SIZE);
        store.setDefault(ISettingsStore.CODE_COMPLETION_LINES_COUNT, ISettingsStore.DEFAULT_CODE_COMPLETION_LINES_COUNT);
        store.setDefault(ISettingsStore.CONTINUOUS_CODE_COMPLETION, true);
    }
}
