/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

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
}
