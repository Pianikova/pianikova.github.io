/**
 *
 */
package com.e1c.edt.ai.tools;

import org.eclipse.core.runtime.IProgressMonitor;

public class ToolProgressMonitor
    implements IProgressMonitor
{
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
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isCanceled()
    {
        return false;
    }

    @Override
    public void setCanceled(boolean value)
    {
        //

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
