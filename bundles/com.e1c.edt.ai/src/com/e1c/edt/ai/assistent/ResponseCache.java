/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ServiceState;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

class ResponseCache<T>
    implements IResponseCache<T>, IAIStateListener
{
    private final ILog log;
    private final Cache<String, CompletableFuture<Optional<T>>> responseCache =
        CacheBuilder.newBuilder().maximumSize(256).build();
    private final Cache<String, CompletableFuture<Optional<T>>> errorsCache =
        CacheBuilder.newBuilder().maximumSize(256).expireAfterWrite(60, TimeUnit.SECONDS).build();

    @Inject
    public ResponseCache(ILog log, IStateService stateService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(stateService);
        this.log = log;
        stateService.addListener(this);
    }

    @SuppressWarnings("nls")
    @Override
    public CompletableFuture<Optional<T>> get(String key,
        Supplier<CompletableFuture<Optional<T>>> responseSupplier,
        boolean reset, boolean cacheErrors)
    {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(responseSupplier);
        try
        {
            synchronized (responseCache)
            {
                if (cacheErrors)
                {
                    var error = errorsCache.getIfPresent(key);
                    if (error != null)
                    {
                        log.trace("ResponseCache", () -> "Returns an error from the cache.");
                        return error;
                    }
                }

                return responseCache.get(key, () -> {
                    return responseSupplier.get().whenComplete((r, e) -> {
                        if (e != null || r.isEmpty())
                        {
                            if (cacheErrors)
                            {
                                errorsCache.put(key, CompletableFuture.failedFuture(e));
                            }

                            responseCache.invalidate(key);
                        }
                        else
                        {
                            errorsCache.invalidate(key);
                        }
                    });
                });
            }
        }
        catch (ExecutionException error)
        {
            log.logError(error);
        }

        return CompletableFuture.completedFuture(Optional.empty());
    }

    @SuppressWarnings("nls")
    @Override
    public synchronized void onStateChange(AIState state)
    {
        var serverState = state.getServiceState();
        if (serverState == ServiceState.SETTINGS_CHANGED)
        {
            synchronized (responseCache)
            {
                responseCache.invalidateAll();
                errorsCache.invalidateAll();
            }

            return;
        }

        if (serverState != ServiceState.ONLINE && serverState != ServiceState.SETTINGS_CHANGED)
        {
            synchronized (responseCache)
            {
                responseCache.invalidateAll();
            }

            log.debug("ResponseCache", () -> "cleared");
        }
    }
}
