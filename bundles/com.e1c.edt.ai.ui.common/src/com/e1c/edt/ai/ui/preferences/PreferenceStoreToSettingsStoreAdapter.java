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
    public Optional<String> getString(String key)
    {
        try
        {
            return Optional.ofNullable(preferenceStore.getString(key));
        }
        catch (Exception error)
        {
            return Optional.empty();
        }
    }

    @Override
    public void setString(String key, String value)
    {
        try
        {
            preferenceStore.setValue(key, value);
        }
        catch (Exception error)
        {
            //
        }
    }

    @Override
    public Optional<Integer> getInt(String key)
    {
        try
        {
            return Optional.ofNullable(preferenceStore.getInt(key));
        }
        catch (Exception error)
        {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Boolean> getBoolean(String key)
    {
        try
        {
            return Optional.ofNullable(preferenceStore.getBoolean(key));
        }
        catch (Exception error)
        {
            return Optional.empty();
        }
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
