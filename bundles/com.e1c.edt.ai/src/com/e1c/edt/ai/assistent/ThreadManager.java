/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.concurrent.CancellationException;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class ThreadManager
    implements IThreadManager
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
        log.trace(Thread.currentThread().getName(), () -> "was canceled", Verbosity.DEFAULT); //$NON-NLS-1$
        throw new CancellationException();
    }
}
