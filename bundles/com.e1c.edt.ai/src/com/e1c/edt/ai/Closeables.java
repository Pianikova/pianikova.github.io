/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;

public class Closeables
{
    public static final AutoCloseable Empty = new Closeable()
    {
        @Override
        public void close()
        {
            // empty
        }
    };

    public static AutoCloseable create(Runnable runnable)
    {
        Preconditions.checkNotNull(runnable);
        var closed = new AtomicReference<>(false);
        return new Closeable() {
            @Override
            public void close()
            {
                if (closed.compareAndSet(false, true))
                {
                    runnable.run();
                }
            }
        };
    }

    public static AutoCloseable create(AutoCloseable... closeables)
    {
        Preconditions.checkNotNull(closeables);
        var closed = new AtomicReference<>(false);
        return new Closeable() {
            @Override
            public void close()
            {
                if (closed.compareAndSet(false, true))
                {
                    for (int i = closeables.length - 1; i >= 0; i--)
                    {
                        try
                        {
                            closeables[i].close();
                        }
                        catch (Exception e)
                        {
                            // ignored
                        }
                    }
                }
            }
        };
    }
}
