/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import org.e1c.edt.ai.ISettingsStore;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceStoreToSettingsStoreAdapter implements ISettingsStore
{
    private IPreferenceStore preferenceStore;

    public PreferenceStoreToSettingsStoreAdapter(IPreferenceStore preferenceStore)
    {
        this.preferenceStore = preferenceStore;
    }

    @Override
    public String getString(String key)
    {
        return preferenceStore.getString(key);
    }

    @Override
    public int getInt(String key)
    {
        return preferenceStore.getInt(key);
    }

}
