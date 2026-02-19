/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.runtime.ListenerList;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.IAIStateListener;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
class StateService
    implements IStateService, IInitializable
{
    private static final ListenerList<IAIStateListener> listeners = new ListenerList<>(ListenerList.IDENTITY);
    private final ILog log;
    private ServiceState serviceState;
    private ActionState actionState;
    private String lastClassOwner;

    @Inject
    public StateService(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    public void initialize()
    {
        setState(StateService.class.getName(), ServiceState.OFFLINE);
    };

    @Override
    public void addListener(IAIStateListener newListener)
    {
        listeners.add(newListener);
    }

    @Override
    public void removeListener(IAIStateListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void setState(String className, ServiceState serviceState)
    {
        if (serviceState == ServiceState.SETTINGS_CHANGED || this.serviceState != serviceState)
        {
            this.lastClassOwner = className;
            this.serviceState = serviceState;
            refresh();
        }
    }

    @Override
    public void setState(String className, ActionState actionState)
    {
        if (this.actionState != actionState)
        {
            this.lastClassOwner = className;
            this.actionState = actionState;
            refresh();
        }
    }

    @Override
    public void refresh()
    {
        if (serviceState == null)
        {
            serviceState = ServiceState.OFFLINE;
        }

        if (actionState == null)
        {
            actionState = ActionState.INACTIVE;
        }

        if (serviceState == ServiceState.OFFLINE)
        {
            actionState = ActionState.INACTIVE;
        }

        var state = new AIState(serviceState, actionState);
        log.trace(TracingSources.API_CALLS, "StateService", () -> state.toString()); //$NON-NLS-1$
        for (var listener : listeners)
        {
            try
            {
                listener.onStateChange(state);
            }
            catch (Throwable error)
            {
                log.logError(error);
            }
        }
    }

    @Override
    public String getLastClassOwner()
    {
        return lastClassOwner;
    }
}

