/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;

public class CancellationTokenSource
    implements ICancellationToken
{
    private static final AtomicInteger ID = new AtomicInteger();
    private final int id = ID.incrementAndGet();
    private final Object lock = new Object();
    private final ArrayList<Runnable> attached = new ArrayList<>();
    private boolean cancelled;

    @Override
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

            for (var runnable : new ArrayList<>(attached))
            {
                runnable.run();
            }

            cancelled = true;
        }
    }

    public static AutoCloseable attach(ICancellationToken cancellationToken, Runnable runnable)
    {
        Preconditions.checkNotNull(cancellationToken);
        Preconditions.checkNotNull(runnable);
        if (!(cancellationToken instanceof CancellationTokenSource))
        {
            return Closeables.Empty;
        }

        var cancellationTokenSource = (CancellationTokenSource)cancellationToken;

        synchronized (cancellationTokenSource.lock)
        {
            cancellationTokenSource.attached.add(runnable);
        }

        return Closeables.create(() -> {
            synchronized (cancellationTokenSource.lock)
            {
                cancellationTokenSource.attached.remove(runnable);
            }
        });
    }

    @Override
    public String toString()
    {
        var sb = new StringBuilder();
        sb.append(id);
        if (isCanceled())
        {
            sb.append(" (cancelled)"); //$NON-NLS-1$
        }

        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        return id;
    }
}