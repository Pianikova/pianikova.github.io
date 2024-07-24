/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.HashMap;
import java.util.Optional;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.client.AISettings;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class SettingsTracker
    implements ISettingsTracker
{
    private final IJson json;
    private final HashMap<String, String> currentSettings = new HashMap<>();

    @Inject
    public SettingsTracker(IJson json)
    {
        Preconditions.checkNotNull(json);
        this.json = json;
    }

    public synchronized int size()
    {
        return currentSettings.size();
    }

    @Override
    public synchronized boolean register(String owner, Optional<AISettings> settings)
    {
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(settings);
        var currentSettnigs = currentSettings.get(owner);
        var newSettings = settings.map(i -> json.serialize(i)).orElse(null);
        if (currentSettnigs == null || !currentSettnigs.equals(newSettings))
        {
            if (newSettings != null)
            {
                currentSettings.put(owner, newSettings);
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
