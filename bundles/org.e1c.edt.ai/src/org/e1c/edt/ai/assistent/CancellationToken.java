/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Nikolay Pyanikov
 *
 */
public class CancellationToken
{
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public Boolean isCanceled()
    {
        return cancelled.get();
    }

    public void cancel()
    {
        cancelled.set(true);
    }

    public void throwIfCanceled()
    {
        if (isCanceled())
        {
            throw new CancellationException();
        }
    }
}
