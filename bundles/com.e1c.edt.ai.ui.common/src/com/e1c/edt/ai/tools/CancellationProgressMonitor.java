/**
 *
 */
package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;

/**
 * Progress monitor that checks for cancellation
 */
public class CancellationProgressMonitor
    implements ICancellationProgressMonitor
{
    private ICancellationToken cancellationToken;
    private boolean isCanceled = false;

    @Override
    public void setCancellationToken(ICancellationToken cancellationToken)
    {
        this.cancellationToken = cancellationToken;
    }

    @Override
    public void beginTask(String name, int totalWork)
    {
        //
    }

    @Override
    public void done()
    {
        //
    }

    @Override
    public void internalWorked(double work)
    {
        //
    }

    @Override
    public boolean isCanceled()
    {
        return isCanceled || cancellationToken.isCanceled();
    }

    @Override
    public void setCanceled(boolean value)
    {
        isCanceled = value;
    }

    @Override
    public void setTaskName(String name)
    {
        //
    }

    @Override
    public void subTask(String name)
    {
        //
    }

    @Override
    public void worked(int work)
    {
        //
    }
}