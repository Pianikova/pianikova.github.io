/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.e1c.edt.ai.assistent.ICheckStatusService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class StatusUpdateJob
    extends Job
{
    private final int pauseTime = 30000;
    private IHealthChecker statusUpdater;
    @Inject
    ICheckStatusService checker;

    public StatusUpdateJob(String name, IHealthChecker statusUpdater)
    {
        super(name);
        this.statusUpdater = statusUpdater;
        Activator.injectMembers(this);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        Optional<Integer> status = Optional.ofNullable(0);
        try
        {
            status = Optional.ofNullable(checker.getStatusAsync().get());
        }
        catch (InterruptedException e)
        {
            Activator.getDefault().logError(e);
        }
        catch (ExecutionException e)
        {
            var activator = Activator.getDefault();
            if (activator != null)
            {
                activator.logError(e);
            }
        }

        status.ifPresent(serverStatus -> Display.getDefault().asyncExec(() -> {
            statusUpdater.setStatus(serverStatus);
        }));

        schedule(pauseTime);

        return Status.OK_STATUS;
    }
}
