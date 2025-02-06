/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ui.eclipse.preferences;

import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.ui.BaseActivator;
import org.e1c.edt.ai.ui.preferences.BaseClientAIPreferencePageInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class ClientAIPreferencePageInitializer
    extends BaseClientAIPreferencePageInitializer
{
    @Override
    public void initializeDefaultPreferences()
    {
        super.initializeDefaultPreferences();
        IPreferenceStore store = BaseActivator.getDefault().getPreferenceStore();
        store.setDefault(ISettingsStore.APIURL, "https://llms.1c.ai/code_java/api/v1/"); //$NON-NLS-1$
    }
}
