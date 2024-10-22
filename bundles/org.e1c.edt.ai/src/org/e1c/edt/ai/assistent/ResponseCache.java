/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.e1c.edt.ai.ServerAccessType;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ResponseCache<T>
    implements IResponseCache<T>, IServerAccessListener
{
    private CompletableFuture<Optional<T>> last;

    @Inject
    public ResponseCache(IServerAccessService serverAccessService)
    {
        Preconditions.checkNotNull(serverAccessService);
        serverAccessService.addServerAccessListener(this);
    }

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

    @Override
    public void onServerAccessChange(ServerAccessType currentStatus)
    {
        if (currentStatus == ServerAccessType.ACCESS_ABSENT)
        {
            reset();
        }
    }
}
