/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
class StateService
    implements IStateService
{
    private static final ListenerList<IAIStateListener> listeners = new ListenerList<>(ListenerList.IDENTITY);
    private final IHealthCheckService healthCheckService;
    private final ILog log;
    private final ISettings settings;
    private ServiceState serviceState;
    private ActionState actionState;

    @Inject
    public StateService(IHealthCheckService healthCheckService, ILog log, ISettings settings)
    {
        Preconditions.checkNotNull(healthCheckService);
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        this.healthCheckService = healthCheckService;
        this.log = log;
        this.settings = settings;
    }

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
            this.serviceState = serviceState;
            refresh();
        }
    }

    @Override
    public void setState(String className, ActionState actionState)
    {
        if (this.actionState != actionState)
        {
            this.actionState = actionState;
            refresh();
        }
    }

    @Override
    public void startMonitoring(int checkPeriodMs, int checkPeriodAfterErrorMs)
    {
        var job = new Job(Messages.UpdatingServerStatus)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try
                {
                    healthCheckService.checkAsync()
                        .orTimeout(settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)
                        .whenComplete((state, error) -> {
                            if (error != null)
                            {
                                log.logError(error);
                                setState(this.getName(), ServiceState.OFFLINE);
                                schedule(checkPeriodAfterErrorMs);
                            }
                            else
                            {
                                schedule(checkPeriodMs);
                            }
                        })
                        .get();
                }
                catch (Throwable error)
                {
                    log.logError(error);
                }

                return Status.OK_STATUS;
            }
        };
        job.setSystem(settings.getVerbosity().getLevel() >= Verbosity.TRACE.getLevel());
        job.setPriority(Job.DECORATE);
        job.schedule();
    };

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
        log.debug("StateService", () -> state.toString()); //$NON-NLS-1$
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
}
