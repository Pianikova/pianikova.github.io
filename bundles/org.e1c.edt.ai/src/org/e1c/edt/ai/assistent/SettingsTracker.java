/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.HashMap;

import com.google.common.base.Preconditions;

public class SettingsTracker
    implements ISettingsTracker
{
    private final HashMap<String, Object> currentSettings = new HashMap<>();

    public synchronized int size()
    {
        return currentSettings.size();
    }

    @Override
    public synchronized boolean register(String owner, Object settings)
    {
        Preconditions.checkNotNull(owner);
        var currentSettnigs = currentSettings.get(owner);
        if (currentSettnigs == null || currentSettnigs.hashCode() != settings.hashCode()
            || !currentSettnigs.equals(settings))
        {
            if (settings != null)
            {
                currentSettings.put(owner, settings);
            }
            else
            {
                currentSettings.remove(owner);
            }

            return true;
        }

        return false;
    }
}
