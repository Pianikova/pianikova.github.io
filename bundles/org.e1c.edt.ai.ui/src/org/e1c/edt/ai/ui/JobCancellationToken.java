/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.CancellationToken;
import org.eclipse.core.runtime.IProgressMonitor;

import com.google.common.base.Preconditions;

public class JobCancellationToken
    extends CancellationToken
{
    private final Object lock = new Object();
    private IProgressMonitor monitor;

    public void attachMonitor(IProgressMonitor monitor)
    {
        Preconditions.checkNotNull(monitor);
        synchronized (lock)
        {
            this.monitor = monitor;
        }
    }

    @Override
    public Boolean isCanceled()
    {
        synchronized (lock)
        {
            return (monitor != null && monitor.isCanceled()) || super.isCanceled();
        }
    }

    @Override
    public void cancel()
    {
        synchronized (lock)
        {
            if (monitor != null)
            {
                monitor.setCanceled(true);
            }
        }

        super.cancel();
    }
}
