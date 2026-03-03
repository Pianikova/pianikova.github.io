/**
 *
 */
package com.e1c.edt.ai.ui;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class Notificator
    implements IStateListener, IInitializable
{
    private static final int REPEAT_INTERVAL_DAYS = 1;

    private final IStateService stateService;
    private final INotifications notifications;
    private final IDispatcher dispatcher;
    private final IClock clock;
    private final Object lock = new Object();
    private ServiceState lastServiceState;
    public ServiceState lastShownServiceState;
    public LocalDateTime lastShownTime;

    @Inject
    public Notificator(IStateService stateService, INotifications notifications, IDispatcher dispatcher, IClock clock)
    {
        Preconditions.checkNotNull(stateService);
        Preconditions.checkNotNull(notifications);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(clock);

        this.stateService = stateService;
        this.notifications = notifications;
        this.dispatcher = dispatcher;
        this.clock = clock;

        stateService.addListener(this);
    }

    @Override
    public void initialize()
    {
        var state = stateService.getState();
        onServiceStateChange(state.getServiceState());
        onActionStateChange(state.getActionState());
        scheduleUpdate(TimeUnit.SECONDS.toMillis(15));
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    public void onServiceStateChange(ServiceState serviceState)
    {
        synchronized (lock)
        {
            if (serviceState == lastServiceState)
            {
                return;
            }

            lastServiceState = serviceState;
        }
    }

    @Override
    public void onActionStateChange(ActionState actionState)
    {
        // Do nothing for action state changes
    }

    private void scheduleUpdate(long delayMs)
    {
        var updateJob = dispatcher.createJob(Messages.UpdateJobMessage, jobCtx -> {
            if (jobCtx.CancellationTokenSource.isCanceled())
            {
                return;
            }

            ServiceState serviceState;
            synchronized (lock)
            {
                serviceState = lastServiceState;
            }

            if (serviceState != null && shouldShowNotification(serviceState) && showState(serviceState))
            {
                synchronized (lock)
                {
                    lastShownServiceState = serviceState;
                    lastShownTime = clock.now();
                }
            }

            scheduleUpdate(TimeUnit.SECONDS.toMillis(5));

        }, true, CancellationTokens.NONE);

        updateJob.setPriority(Job.DECORATE);
        updateJob.setSystem(true);
        updateJob.schedule(delayMs);
    }

    public boolean shouldShowNotification(ServiceState serviceState)
    {
        synchronized (lock)
        {
            if (serviceState == null)
            {
                return false;
            }

            // MISSING_TOKEN never shows repeatedly
            if (serviceState == ServiceState.MISSING_TOKEN)
            {
                return lastShownServiceState != serviceState;
            }

            // For other states, check if repeat interval has passed
            if (lastShownServiceState == serviceState && lastShownTime != null)
            {
                return clock.now().isAfter(lastShownTime.plusDays(REPEAT_INTERVAL_DAYS));
            }

            return true;
        }
    }

    private boolean showState(ServiceState serviceState)
    {
        switch (serviceState)
        {
        case MISSING_TOKEN:
            return notifications.showMissingTokenInfo();

        case TOKEN_ERROR:
            return notifications.showTokenError();

        case SSL_ERROR:
            return notifications.showSSLError();

        default:
            return true;
        }
    }
}
