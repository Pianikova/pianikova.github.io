/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.preferences;

import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.ui.BaseActivator;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * This class contains default parameters on the preferences page with the
 * AI chat settings. Default parameters allows user to test standart version of the AI.
 * @author Bogdan Sushkov
 *
 */
public class BaseClientAIPreferencePageInitializer
    extends AbstractPreferenceInitializer
{
    @Override
    public void initializeDefaultPreferences()
    {
        IPreferenceStore store = BaseActivator.getDefault().getPreferenceStore();
        store.setDefault(ISettingsStore.CODE_COMPLETION, true);
        store.setDefault(ISettingsStore.CLIENT_TOKEN, ""); //$NON-NLS-1$
        store.setDefault(ISettingsStore.CONTINUOUS_CODE_COMPLETION, true);
        store.setDefault(ISettingsStore.PARAMETERS, ""); //$NON-NLS-1$
        store.setDefault(ISettingsStore.CODE_COMPLETION_LINES_COUNT, ISettingsStore.DEFAULT_CODE_COMPLETION_LINES_COUNT);
    }
}
