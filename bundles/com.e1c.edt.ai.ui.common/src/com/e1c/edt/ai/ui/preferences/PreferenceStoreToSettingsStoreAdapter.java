/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.preferences;

import java.util.Optional;

import org.eclipse.jface.preference.IPreferenceStore;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ISettingsStore;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class PreferenceStoreToSettingsStoreAdapter implements ISettingsStore
{
    private final IPreferenceStore preferenceStore;
    private final IJson json;

    @Inject
    public PreferenceStoreToSettingsStoreAdapter(IPreferenceStore preferenceStore, IJson json)
    {
        Preconditions.checkNotNull(preferenceStore);
        Preconditions.checkNotNull(json);
        this.preferenceStore = preferenceStore;
        this.json = json;
    }

    @Override
    public String getString(String key)
    {
        return preferenceStore.getString(key);
    }

    @Override
    public void setString(String key, String value)
    {
        preferenceStore.setValue(key, value);
    }

    @Override
    public int getInt(String key)
    {
        return preferenceStore.getInt(key);
    }

    @Override
    public boolean getBoolean(String key)
    {
        return preferenceStore.getBoolean(key);
    }

    @Override
    public <T> Optional<T> getValue(String key, Class<T> classOfT)
    {
        var value = preferenceStore.getString(key);
        if (value == null)
        {
            return Optional.empty();
        }

        return json.deserialize(value, classOfT);
    }

    @Override
    public <T> void setValue(String key, T value)
    {
        var serializedValue = json.serialize(value);
        preferenceStore.setValue(key, serializedValue);
    }
}
