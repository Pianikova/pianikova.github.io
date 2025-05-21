/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IGlobalContext;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.IGlobalContextService;
import com.e1c.edt.ai.assistent.model.EntityKey;
import com.e1c.edt.ai.assistent.model.EntityValue;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdateResponse;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class GlobalContextSync implements IGlobalContextSync
{
    private final ILog log;
    private final Provider<IStatistics> statisticsProvider;
    private final IJson json;
    private final IGlobalContext globalContext;
    private final IGlobalContextService globalContextService;

    @Inject
    public GlobalContextSync(ILog log, Provider<IStatistics> statisticsProvider, IJson json,
        IGlobalContext globalContext, IGlobalContextService globalContextService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(globalContext);
        Preconditions.checkNotNull(globalContextService);
        this.log = log;
        this.statisticsProvider = statisticsProvider;
        this.json = json;
        this.globalContext = globalContext;
        this.globalContextService = globalContextService;
    }

    @Override
    public CompletableFuture<Boolean> sync(ProjectId projectId, String filePath, int maxDept,
        ICancellationToken cancellationToken)
    {
        try
        {
            var statistics = statisticsProvider.get();
            var updates = globalContext.getUpdates(projectId, filePath, false, statistics, cancellationToken);
            return syncUpdates(projectId, updates, maxDept, statistics, cancellationToken);
        }
        catch (Exception error)
        {
            log.logError(error);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public CompletableFuture<Boolean> syncUpdates(ProjectId projectId, List<GlobalContextUpdate> updates,
        int maxDept,
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

            return globalContextService.update(projectId, updates, 10, statistics, cancellationToken)
                .thenApplyAsync(optionalResult -> {
                    if (optionalResult.isEmpty())
                    {
                        return false;
                    }

                    var result = optionalResult.get();
                    return syncUnknown(projectId, result.unknownValues, result.unknownKeys, maxDept,
                        cancellationToken);
                });
        }
        catch (Exception error)
        {
            log.logError(error);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public boolean syncUnknown(ProjectId projectId, List<EntityValue> unknownValues,
        List<EntityKey> unknownKeys, int maxDept,
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

                log.debug("AI global context is needed " + cancellationToken.toString(), () -> { //$NON-NLS-1$
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

                var fileUpdates = new HashMap<String, FileUpdates>();
                if (vals != null)
                {
                    for (var val : vals)
                    {
                        var fileUpdate = fileUpdates.computeIfAbsent(val.path, k -> new FileUpdates());
                        fileUpdate.hashes.add(val.hash);
                        fileUpdate.fields.add(val.field);
                    }
                }

                if (keys != null)
                {
                    for (var key : keys)
                    {
                        var fileUpdate = fileUpdates.computeIfAbsent(key.path, k -> new FileUpdates());
                        fileUpdate.fields.add(key.field);
                    }
                }

                if (fileUpdates.isEmpty())
                {
                    return true;
                }

                var allUpdates = new ArrayList<GlobalContextUpdate>();
                for (var fileUpdate : fileUpdates.entrySet())
                {
                    var path = fileUpdate.getKey();
                    var data = fileUpdate.getValue();
                    var updates = globalContext.getUpdates(projectId,
                        path, data.hashes, data.fields, statistics, cancellationToken);
                    allUpdates.addAll(updates);
                }

                if (allUpdates.isEmpty())
                {
                    return true;
                }

                if (cancellationToken.isCanceled())
                {
                    return true;
                }

                optionalResult =
                    globalContextService.update(projectId, allUpdates, 10, statistics, cancellationToken).get();
            }
        }
        catch (Exception error)
        {
            log.logError(error);
            return false;
        }

        return true;
    }

    private static class FileUpdates
    {
        public final HashSet<String> hashes = new HashSet<>();

        public final HashSet<String> fields = new HashSet<>();
    }
}
