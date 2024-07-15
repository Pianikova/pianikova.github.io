/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ResponseCache<T>
    implements IResponseCache<T>
{
    private CompletableFuture<Optional<T>> last;

    @Override
    public synchronized CompletableFuture<Optional<T>> get(Supplier<CompletableFuture<Optional<T>>> responseSupplier,
        boolean reset)
    {
        if (!reset && last != null)
        {
            return last;
        }

        last = responseSupplier.get().whenComplete((r, e) -> {
            if (e != null || r.isEmpty())
            {
                reset();
            }
        });

        return last;
    }

    private void reset()
    {
        last = null;
    }
}
