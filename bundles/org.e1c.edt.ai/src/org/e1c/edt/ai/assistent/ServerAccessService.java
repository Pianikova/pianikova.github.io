/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.ServerAccessType;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class ServerAccessService
    implements IServerAccessService
{
    private ServerAccessType access;
    private static final ListenerList<IServerAccessListener> listeners = new ListenerList<>(ListenerList.IDENTITY);
    private final ICheckStatusService checker;
    private final ILog log;
    private final IUISettings uiSettings;

    @Inject
    public ServerAccessService(ICheckStatusService checker, ILog log, IUISettings uiSettings)
    {
        Preconditions.checkNotNull(checker);
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(uiSettings);
        this.checker = checker;
        this.log = log;
        this.uiSettings = uiSettings;
    }

    @Override
    public void addServerAccessListener(IServerAccessListener newListener)
    {
        listeners.add(newListener);
    }

    public void removeServerAccessListener(IServerAccessListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public synchronized void accessChanged(String className, ServerAccessType status)
    {
        if (access != status)
        {
            access = status;
            notifyListeners(access);
        }
    }

    @Override
    public void startMonitoring(int checkPeriodMs, int checkPeriodAfterErrorMs)
    {
        var jobName = "Updating server status..."; //$NON-NLS-1$
        var job = new Job(jobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                Optional<Integer> status = Optional.empty();
                try
                {
                    status =
                        Optional.ofNullable(checker.getStatusAsync()
                            .orTimeout(uiSettings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)
                            .get());
                }
                catch (Throwable e)
                {
                    log.logError(e);
                }

                status.ifPresentOrElse(
                    statusCode -> {
                        accessChanged(this.getName(),
                            statusCode >= 400 ? ServerAccessType.ACCESS_ABSENT : ServerAccessType.ACCESS_PRESENT);

                        schedule(checkPeriodMs);
                    },
                    () -> {
                        log.logError(this.getName() + ": Server dose not return status"); //$NON-NLS-1$
                        if (access == ServerAccessType.ACCESS_PRESENT)
                        {
                            accessChanged(this.getName(), ServerAccessType.ACCESS_ABSENT);
                        }

                        schedule(checkPeriodAfterErrorMs);
                    });
                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.DECORATE);
        job.schedule();
    };

    private void notifyListeners(ServerAccessType access)
    {
        for (var listener : listeners)
        {
            try
            {
                listener.onServerAccessChange(access);
            }
            catch (Throwable error)
            {
                log.logError(error);
            }
        }
    }
}
