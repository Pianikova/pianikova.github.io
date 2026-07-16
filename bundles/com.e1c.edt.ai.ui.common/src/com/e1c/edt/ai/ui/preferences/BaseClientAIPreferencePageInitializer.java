/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.ProblemLevel;
import com.e1c.edt.ai.ui.BaseActivator;

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
        store.setDefault(ISettingsStore.CODE_COMPLETION_POLICY, CodeCompletionPolicy.MODERATE.getId());
        store.setDefault(ISettingsStore.CLIENT_TOKEN, ""); //$NON-NLS-1$
        store.setDefault(ISettingsStore.PARAMETERS, ""); //$NON-NLS-1$
        store.setDefault(ISettingsStore.CODE_COMPLETION_LINES_COUNT, ISettingsStore.DEFAULT_CODE_COMPLETION_LINES_COUNT);
        store.setDefault(ISettingsStore.SHOW_STATUS_BAR, true);
        store.setDefault(ISettingsStore.SHOW_ACTIVATION_INFO, true);
        store.setDefault(ISettingsStore.AUTO_OPEN_DIFF_PREVIEW, true);
        store.setDefault(ISettingsStore.BACKGROUND_ANALYSIS, true);
        store.setDefault(ISettingsStore.BACKGROUND_ANALYSIS_PROBLEM_LEVEL, ProblemLevel.WARNING.getId());
    }
}
