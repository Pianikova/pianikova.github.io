/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.CancellationTokenSource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.google.common.base.Preconditions;

class JobCancellationTokenSource
    extends CancellationTokenSource
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
