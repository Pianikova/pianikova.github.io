/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.model.Session;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

class ResponseCache
    implements IResponseCache, IStateListener
{
    private final ILog log;
    private static final Object GLOBAL_SESSION_KEY = new Object();
    private final Cache<Object, CompletableFuture<Optional<Session>>> responseCache =
        CacheBuilder.newBuilder().maximumSize(256).build();

    @Inject
    public ResponseCache(ILog log, IStateService stateService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(stateService);
        this.log = log;
        stateService.addListener(this);
    }

    @Override
    public CompletableFuture<Optional<Session>> get(IProject project,
        Supplier<CompletableFuture<Optional<Session>>> taskSupplier, boolean reset)
    {
        Preconditions.checkNotNull(project);
        return get(project, taskSupplier);
    }

    @Override
    public CompletableFuture<Optional<Session>> getGlobal(
        Supplier<CompletableFuture<Optional<Session>>> taskSupplier, boolean reset)
    {
        return get(GLOBAL_SESSION_KEY, taskSupplier);
    }

    private CompletableFuture<Optional<Session>> get(Object key,
        Supplier<CompletableFuture<Optional<Session>>> taskSupplier)
    {
        Preconditions.checkNotNull(taskSupplier);
        try
        {
            return responseCache.get(key, () -> {
                return taskSupplier.get().whenComplete((r, e) -> {
                    if (e != null || r.isEmpty())
                    {
                        responseCache.invalidate(key);
                    }
                });
            });
        }
        catch (ExecutionException error)
        {
            log.logError(error);
        }

        return CompletableFuture.completedFuture(Optional.empty());
    }

    @SuppressWarnings({ "nls", "incomplete-switch" })
    @Override
    public synchronized void onServiceStateChange(ServiceState serviceState)
    {
        switch (serviceState)
        {
        case SETTINGS_CHANGED:
        case SESSION_EXPIRED:
            responseCache.invalidateAll();
            log.trace(TracingSources.API_CALLS, "ResponseCache", () -> "cleared");
            break;
        }
    }

    @Override
    public void onActionStateChange(ActionState actionState)
    {
        // Do nothing for action state changes
    }
}
