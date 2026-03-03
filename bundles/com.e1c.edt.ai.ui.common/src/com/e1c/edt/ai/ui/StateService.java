/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.ListenerList;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.IStateListener;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
class StateService
    implements IStateService
{
    private static final ListenerList<IStateListener> listeners = new ListenerList<>(ListenerList.IDENTITY);
    private final ILog log;
    private final ISettings settings;
    private ServiceState serviceState = ServiceState.NONE;
    private ActionState actionState = ActionState.INACTIVE;
    private AtomicInteger _busy = new AtomicInteger(0);

    @Inject
    public StateService(ILog log, ISettings settings)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);

        this.log = log;
        this.settings = settings;
    }

    @Override
    public void addListener(IStateListener newListener)
    {
        listeners.add(newListener);
        refresh();
    }

    @Override
    public void removeListener(IStateListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public AIState getState()
    {
        var curState = serviceState;
        if (!settings.hasClientToken())
        {
            curState = ServiceState.MISSING_TOKEN;
        }

        return new AIState(curState, actionState);
    }

    @Override
    public void setState(ServiceState serviceState)
    {
        Preconditions.checkNotNull(serviceState);
        if (!settings.hasClientToken())
        {
            serviceState = ServiceState.MISSING_TOKEN;
        }

        if (this.serviceState != serviceState || serviceState.isAllowDuplicates())
        {
            this.serviceState = serviceState;
            notifyServiceStateChanged();
        }
    }

    @Override
    public void refresh()
    {
        var state = getState();
        log.trace(TracingSources.API_CALLS, "StateService", () -> state.toString()); //$NON-NLS-1$
        notifyServiceStateChanged();
        notifyActionStateChanged();
    }

    @Override
    public AutoCloseable busy()
    {
        if (_busy.incrementAndGet() == 1)
        {
            setState(ActionState.BUSY);
        }

        return Closeables.create(() -> {
            if (_busy.decrementAndGet() == 0)
            {
                setState(ActionState.INACTIVE);
            }
        });
    }

    private void setState(ActionState actionState)
    {
        Preconditions.checkNotNull(actionState);
        if (this.actionState != actionState)
        {
            this.actionState = actionState;
            notifyActionStateChanged();
        }
    }

    private void notifyServiceStateChanged()
    {
        var serviceState = getState().getServiceState();
        for (var listener : listeners)
        {
            try
            {
                listener.onServiceStateChange(serviceState);
            }
            catch (Throwable error)
            {
                log.logError(error);
            }
        }
    }

    private void notifyActionStateChanged()
    {
        var actionState = getState().getActionState();
        for (var listener : listeners)
        {
            try
            {
                listener.onActionStateChange(actionState);
            }
            catch (Throwable error)
            {
                log.logError(error);
            }
        }
    }
}
