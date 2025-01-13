/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.e1c.edt.ai.ServerAccessType;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class ResponseCache<T>
    implements IResponseCache<T>, IServerAccessListener
{
    private final ConcurrentHashMap<String, CompletableFuture<Optional<T>>> last = new ConcurrentHashMap<>();

    @Inject
    public ResponseCache(IServerAccessService serverAccessService)
    {
        Preconditions.checkNotNull(serverAccessService);
        serverAccessService.addServerAccessListener(this);
    }

    @Override
    public synchronized CompletableFuture<Optional<T>> get(String key,
        Supplier<CompletableFuture<Optional<T>>> responseSupplier,
        boolean reset)
    {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(responseSupplier);
        return last.compute(key, (k, val) -> {
            if (!reset && val != null)
            {
                return val;
            }

            return responseSupplier.get().whenComplete((r, e) -> {
                if (e != null || r.isEmpty())
                {
                    reset(key);
                }
            });
        });
    }

    private void reset(String key)
    {
        last.remove(key);
    }

    @Override
    public void onServerAccessChange(ServerAccessType currentStatus)
    {
        if (currentStatus == ServerAccessType.ACCESS_ABSENT)
        {
            last.clear();
        }
    }
}
