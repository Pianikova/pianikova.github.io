/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.concurrent.CancellationException;

import org.e1c.edt.ai.ILog;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ThreadManager implements IThreadManager
{
    private final ILog log;

    @Inject
    public ThreadManager(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    public void cancel()
    {
        log.trace(Thread.currentThread().getName(), "was canceled"); //$NON-NLS-1$
        throw new CancellationException();
    }
}
