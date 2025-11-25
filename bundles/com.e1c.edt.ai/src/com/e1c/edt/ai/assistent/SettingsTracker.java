/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.HashMap;

import org.eclipse.core.runtime.ListenerList;

import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class SettingsTracker
    implements ISettingsTracker
{
    @Inject
    private IStateService stateService;
    private final HashMap<String, Object> currentSettings = new HashMap<>();
    private static final ListenerList<IAISettingsListener> listeners = new ListenerList<>(ListenerList.IDENTITY);

    public synchronized int size()
    {
        return currentSettings.size();
    }

    @Override
    public synchronized boolean register(String owner, Object settings)
    {
        Preconditions.checkNotNull(owner);
        Preconditions.checkNotNull(settings);
        var currentSettings = this.currentSettings.get(owner);
        if (currentSettings == null || currentSettings.hashCode() != settings.hashCode()
            || !currentSettings.equals(settings))
        {
            if (stateService != null)
            {
                stateService.setState(this.getClass().getName(), ServiceState.SETTINGS_CHANGED);
            }
            notifyListeners();
            this.currentSettings.put(owner, settings);
            return true;
        }

        return false;
    }

    private void notifyListeners()
    {
        for (var listener : listeners)
        {
            listener.onSettingsChanged();
        }

    }

    @Override
    public void addListener(IAISettingsListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IAISettingsListener listener)
    {
        listeners.remove(listener);
    }
}