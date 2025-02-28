/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IGlobalContextFactory;
import com.e1c.edt.ai.IGlobalContextRequestFactory;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.IGlobalContextService;
import com.e1c.edt.ai.assistent.model.EntityKey;
import com.e1c.edt.ai.assistent.model.EntityValue;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdateResponse;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class GlobalContextSync implements IGlobalContextSync
{
    private final ILog log;
    private final Provider<IStatistics> statisticsProvider;
    private final IJson json;
    private final IGlobalContextRequestFactory globalContextRequestFactory;
    private final IGlobalContextService globalContextService;
    private final IGlobalContextFactory globalContextFactory;

    @Inject
    public GlobalContextSync(ILog log, Provider<IStatistics> statisticsProvider, IJson json,
        IGlobalContextRequestFactory globalContextRequestFactory, IGlobalContextService globalContextService,
        IGlobalContextFactory globalContextFactory)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(globalContextRequestFactory);
        Preconditions.checkNotNull(globalContextService);
        Preconditions.checkNotNull(globalContextFactory);
        this.log = log;
        this.statisticsProvider = statisticsProvider;
        this.json = json;
        this.globalContextRequestFactory = globalContextRequestFactory;
        this.globalContextService = globalContextService;
        this.globalContextFactory = globalContextFactory;
    }

    @Override
    public CompletableFuture<Boolean> sync(AIContext aiCtx, int maxDept, ICancellationToken cancellationToken)
    {
        try
        {
            var statistics = statisticsProvider.get();
            var updates = getSyncData(aiCtx, statistics, cancellationToken);
            return sync(aiCtx, updates, maxDept, statistics, cancellationToken);
        }
        catch (Exception error)
        {
            log.logError(error);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public List<GlobalContextUpdate> getSyncData(AIContext aiCtx, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var globalContext = globalContextFactory.createGlobalContext(aiCtx, statistics, cancellationToken);
        return globalContextRequestFactory.createGlobalContextUpdates(aiCtx, globalContext, statistics,
            cancellationToken);
    }

    @Override
    public CompletableFuture<Boolean> sync(AIContext aiCtx, List<GlobalContextUpdate> updates, int maxDept,
        IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        try
        {
            if (updates.isEmpty())
            {
                return CompletableFuture.completedFuture(true);
            }

            if (cancellationToken.isCanceled())
            {
                return CompletableFuture.completedFuture(false);
            }

            return globalContextService.update(aiCtx.getProjectId(), updates, statistics, cancellationToken)
                .thenApplyAsync(optionalResult -> {
                    if (optionalResult.isEmpty())
                    {
                        return false;
                    }

                    var result = optionalResult.get();
                    return sync(aiCtx, result.unknownValues, result.unknownKeys, maxDept, cancellationToken);
                });
        }
        catch (Exception error)
        {
            log.logError(error);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public boolean sync(AIContext aiCtx, List<EntityValue> unknownValues, List<EntityKey> unknownKeys, int maxDept,
        ICancellationToken cancellationToken)
    {
        try
        {
            if (cancellationToken.isCanceled())
            {
                return false;
            }

            var statistics = statisticsProvider.get();
            var response = new GlobalContextUpdateResponse();
            response.unknownValues = unknownValues;
            response.unknownKeys = unknownKeys;
            var optionalResult = Optional.ofNullable(response);
            while (maxDept-- > 0 && optionalResult.isPresent())
            {
                var result = optionalResult.get();
                var vals = result.unknownValues;
                var keys = result.unknownKeys;
                var hasUnknownValues = vals != null && !vals.isEmpty();
                var hasUnknownKeys = keys != null && !keys.isEmpty();
                if (!hasUnknownValues && !hasUnknownKeys)
                {
                    return true;
                }

                log.trace("AI global context is needed " + cancellationToken.toString(), () -> { //$NON-NLS-1$
                    var trace = new StringBuilder();
                    if (hasUnknownValues)
                    {
                        trace.append("Unknown values:"); //$NON-NLS-1$
                        trace.append(System.lineSeparator());
                        trace.append(json.serialize(vals));
                    }

                    if (hasUnknownKeys)
                    {
                        if (trace.length() > 0)
                        {
                            trace.append(System.lineSeparator());
                        }

                        trace.append("Unknown keys:"); //$NON-NLS-1$
                        trace.append(System.lineSeparator());
                        trace.append(json.serialize(keys));
                    }

                    return trace.toString();
                });

                var hashes = new HashSet<String>();
                if (vals != null)
                {
                    for (var val : vals)
                    {
                        hashes.add(val.hash);
                    }
                }

                var fields = new HashSet<String>();
                if (keys != null)
                {
                    for (var key : keys)
                    {
                        fields.add(key.field);
                    }
                }

                if (hashes.isEmpty() && fields.isEmpty())
                {
                    return true;
                }

                var updates = globalContextRequestFactory.createGlobalContextUpdates(aiCtx, hashes, fields, statistics,
                    cancellationToken);

                if (updates.isEmpty())
                {
                    return true;
                }

                if (cancellationToken.isCanceled())
                {
                    return true;
                }

                optionalResult =
                    globalContextService.update(aiCtx.getProjectId(), updates, statistics, cancellationToken).get();
            }
        }
        catch (Exception error)
        {
            log.logError(error);
            return false;
        }

        return true;
    }
}
