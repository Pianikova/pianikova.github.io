/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;

import com.google.common.base.Preconditions;

public class CancellationToken
{
    public final static CancellationToken NONE = new CancellationToken()
    {
        @Override
        public void cancel()
        {
            //
        }

        @Override
        public Boolean isCanceled()
        {
            return false;
        }
    };

    private final Object lock = new Object();
    private final ArrayList<Runnable> attached = new ArrayList<>();
    private boolean cancelled;

    public Boolean isCanceled()
    {
        synchronized (lock)
        {
            return cancelled;
        }
    }

    public void cancel()
    {
        synchronized (lock)
        {
            if (cancelled)
            {
                return;
            }

            for (var runnable : attached)
            {
                runnable.run();
            }

            cancelled = true;
        }
    }

    public void throwIfCanceled()
    {
        if (isCanceled())
        {
            throw new CancellationException();
        }
    }

    public AutoCloseable attach(Runnable runnable)
    {
        Preconditions.checkNotNull(runnable);
        synchronized (lock)
        {
            attached.add(runnable);
        }

        return Closeables.create(() -> {
            synchronized (lock)
            {
                attached.remove(runnable);
            }
        });
    }

    @Override
    public String toString()
    {
        var sb = new StringBuilder();
        sb.append(hashCode());
        if (isCanceled())
        {
            sb.append(" (cancelled)"); //$NON-NLS-1$
        }

        return sb.toString();
    }
}