/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ServiceState;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class ResponseCache<T>
    implements IResponseCache<T>, IAIStateListener
{
    private final ILog log;
    private final ConcurrentHashMap<String, CompletableFuture<Optional<T>>> last = new ConcurrentHashMap<>();

    @Inject
    public ResponseCache(ILog log, IStateService stateService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(stateService);
        this.log = log;
        stateService.addListener(this);
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

    @SuppressWarnings("nls")
    @Override
    public void onStateChange(AIState state)
    {
        var serverState = state.getServiceState();
        if (serverState != ServiceState.ONLINE && serverState != ServiceState.SETTINGS_CHANGED)
        {
            log.debug("ResponseCache", () -> "cleared");
            last.clear();
        }
    }
}
