/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.IAIStateListener;
import com.e1c.edt.ai.assistent.IHealthCheckService;
import com.e1c.edt.ai.assistent.Messages;
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
    private final IHealthCheckService healthCheckService;
    private final ILog log;
    private final ISettings settings;
    private final IDispatcher dispatcher;
    private ServiceState serviceState;
    private ActionState actionState;
    private String lastClassOwner;

    @Inject
    public StateService(IHealthCheckService healthCheckService, ILog log, ISettings settings, IDispatcher dispatcher)
    {
        Preconditions.checkNotNull(healthCheckService);
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(dispatcher);
        this.healthCheckService = healthCheckService;
        this.log = log;
        this.settings = settings;
        this.dispatcher = dispatcher;
    }

    @Override
    public void initialize()
    {
        startMonitoring(30000, 5000, 0);
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

    private void startMonitoring(int checkPeriodMs, int checkPeriodAfterErrorMs, int periodMs)
    {
        var job = dispatcher.createJob(Messages.UpdatingServerStatus, jobCtx -> {
            if (jobCtx.CancellationTokenSource.isCanceled())
            {
                return;
            }

            if (!settings.isEnabled())
            {
                startMonitoring(checkPeriodMs, checkPeriodAfterErrorMs, checkPeriodMs);
                return;
            }

            try
            {
                var ckectTask = healthCheckService.checkAsync()
                    .orTimeout(settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)
                    .whenComplete((state, error) -> {
                        if (error != null)
                        {
                            log.logError(error);
                            setState(Messages.UpdatingServerStatus, ServiceState.OFFLINE);
                            startMonitoring(checkPeriodMs, checkPeriodAfterErrorMs, checkPeriodAfterErrorMs);
                        }
                        else
                        {
                            startMonitoring(checkPeriodMs, checkPeriodAfterErrorMs, checkPeriodMs);
                        }
                    });

                CancellationTokenSource.attach(jobCtx.CancellationTokenSource, () -> ckectTask.cancel(true));
                ckectTask.get();
            }
            catch (Throwable error)
            {
                log.logError(error);
            }
        }, true, CancellationTokens.NONE);

        job.setSystem(true);
        job.setPriority(Job.DECORATE);
        job.schedule(periodMs);
    }
}
