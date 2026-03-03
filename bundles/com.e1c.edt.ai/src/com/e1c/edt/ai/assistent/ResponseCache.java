/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Session;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

class ResponseCache
    implements IResponseCache, IStateListener
{
    private final ILog log;
    private final Cache<ProjectId, CompletableFuture<Optional<Session>>> responseCache =
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
    public CompletableFuture<Optional<Session>> get(ProjectId projectId,
        Supplier<CompletableFuture<Optional<Session>>> taskSupplier, boolean reset)
    {
        Preconditions.checkNotNull(projectId);
        Preconditions.checkNotNull(taskSupplier);
        try
        {
            synchronized (responseCache)
            {
                return responseCache.get(projectId, () -> {
                    return taskSupplier.get().whenComplete((r, e) -> {
                        if (e != null || r.isEmpty())
                        {
                            responseCache.invalidate(projectId);
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

    @SuppressWarnings({ "nls", "incomplete-switch" })
    @Override
    public synchronized void onServiceStateChange(ServiceState serviceState)
    {
        switch (serviceState)
        {
        case SETTINGS_CHANGED:
        case SESSION_EXPIRED:
            synchronized (responseCache)
            {
                responseCache.invalidateAll();
            }

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
