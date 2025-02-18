/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.e1c.edt.ai.AIState;
import org.e1c.edt.ai.ServiceState;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class ResponseCache<T>
    implements IResponseCache<T>, IAIStateListener
{
    private final ConcurrentHashMap<String, CompletableFuture<Optional<T>>> last = new ConcurrentHashMap<>();

    @Inject
    public ResponseCache(IStateService stateService)
    {
        Preconditions.checkNotNull(stateService);
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

    @Override
    public void onStateChange(AIState state)
    {
        if (state.getServiceState() == ServiceState.OFFLINE)
        {
            last.clear();
        }
    }
}
