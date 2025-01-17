/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.CancellationTokenSource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.google.common.base.Preconditions;

public class JobContext
{
    public final IProgressMonitor Monitor;
    public final CancellationTokenSource CancellationTokenSource;

    public JobContext(IProgressMonitor monitor, CancellationTokenSource сancellationTokenSource)
    {
        Preconditions.checkNotNull(monitor);
        Preconditions.checkNotNull(сancellationTokenSource);
        Monitor = monitor;
        CancellationTokenSource = сancellationTokenSource;
    }
}
