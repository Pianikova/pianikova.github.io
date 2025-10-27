/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.CancellationTokenSource;
import com.google.common.base.Preconditions;

public class JobContext
{
    public final Job Job;
    public final IProgressMonitor Monitor;
    public final CancellationTokenSource CancellationTokenSource;

    public JobContext(Job job, IProgressMonitor monitor, CancellationTokenSource cancellationTokenSource)
    {
        Preconditions.checkNotNull(job);
        Preconditions.checkNotNull(monitor);
        Preconditions.checkNotNull(cancellationTokenSource);
        Job = job;
        Monitor = monitor;
        CancellationTokenSource = cancellationTokenSource;
    }
}
