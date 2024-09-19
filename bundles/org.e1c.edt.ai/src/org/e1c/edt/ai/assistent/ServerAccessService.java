/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.e1c.edt.ai.ILog;
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


    @Inject
    public ServerAccessService(ICheckStatusService checker, ILog log)
    {
        Preconditions.checkNotNull(checker);
        Preconditions.checkNotNull(log);
        this.checker = checker;
        this.log = log;
    }

    @Override
    public void addServerAccessListener(ServerAccessListener newListener)
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
    public void startMonitoring(int pauseTime)
    {
        var jobName = "Updating server status..."; //$NON-NLS-1$
        var job = new Job(jobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                Optional<Integer> status = Optional.ofNullable(0);
                try
                {
                    status = Optional.ofNullable(checker.getStatusAsync().get());
                }
                catch (InterruptedException e)
                {
                    log.logError(e);
                }
                catch (ExecutionException e)
                {
                    log.logError(e);
                }

                status.ifPresentOrElse(
                    serverStatus -> {
                        if (serverStatus >= 200 && serverStatus < 300)
                        {
                            accessChanged(this.getName(), ServerAccessType.ACCESS_PRESENT);
                        }
                        else
                        {
                            accessChanged(this.getName(), ServerAccessType.ACCESS_ABSENT);
                        }
                    },
                    () -> {
                        log.logError(this.getName() + ": Server returned null-status"); //$NON-NLS-1$
                        if (access == ServerAccessType.ACCESS_PRESENT)
                        {
                            accessChanged(this.getName(), ServerAccessType.ACCESS_ABSENT);
                        }
                    });
                schedule(pauseTime);
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
            listener.onServerAccessChange(access);
        }
    }
}
