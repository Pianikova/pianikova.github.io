/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.assistent.CancellationToken;
import org.eclipse.core.runtime.IProgressMonitor;

public class JobCancellationToken
    extends CancellationToken
{
    private IProgressMonitor monitor;

    public JobCancellationToken(IProgressMonitor monitor)
    {
        this.monitor = monitor;
    }

    @Override
    public Boolean isCanceled()
    {
        return monitor.isCanceled() || super.isCanceled();
    }

    @Override
    public void cancel()
    {
        monitor.setCanceled(true);
        super.cancel();
    }
}
