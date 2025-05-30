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

import com.e1c.edt.ai.AIContext;
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
    public CompletableFuture<Boolean> sync(AIContext aiContext, int maxDept,
        ICancellationToken cancellationToken)
    {
        try
        {
            var statistics = statisticsProvider.get();
            var updates = globalContext.getUpdates(aiContext, false, statistics, cancellationToken);
            return syncUpdates(aiContext, false, updates, maxDept, statistics, cancellationToken);
        }
        catch (Exception error)
        {
            log.logError(error);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public CompletableFuture<Boolean> syncUpdates(AIContext aiContext, boolean isInitial,
        List<GlobalContextUpdate> updates,
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

            return globalContextService.update(aiContext.getProjectId(), updates, 200, statistics, cancellationToken)
                .thenCompose(optionalResult -> {
                    if (optionalResult.isEmpty())
                    {
                        return CompletableFuture.completedFuture(false);
                    }

                    var result = optionalResult.get();
                    if (result.isEmpty())
                    {
                        return CompletableFuture.completedFuture(true);
                    }

                    return syncUnknown(aiContext, isInitial, result.unknownValues, result.unknownKeys, maxDept,
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
    public CompletableFuture<Boolean> syncUnknown(AIContext aiContext, boolean isInitial,
        List<EntityValue> unknownValues,
        List<EntityKey> unknownKeys, int maxDept,
        ICancellationToken cancellationToken)
    {
        var feature = CompletableFuture.completedFuture(true);
        try
        {
            if (cancellationToken.isCanceled())
            {
                return CompletableFuture.completedFuture(false);
            }

            var statistics = statisticsProvider.get();
            var response = new GlobalContextUpdateResponse();
            response.unknownValues = unknownValues;
            response.unknownKeys = unknownKeys;
            var optionalResult = Optional.ofNullable(response);
            while (maxDept-- > 0 && optionalResult.isPresent())
            {
                var result = optionalResult.get();
                if (result.isEmpty())
                {
                    return feature;
                }

                var vals = result.unknownValues;
                var keys = result.unknownKeys;
                var hasUnknownValues = vals != null && !vals.isEmpty();
                var hasUnknownKeys = keys != null && !keys.isEmpty();
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
                        var fileUpdate = fileUpdates.computeIfAbsent(val.path, path -> new FileUpdates(path, val.hash));
                        fileUpdate.hashes.add(val.hash);
                        fileUpdate.fields.add(val.field);
                    }
                }

                if (keys != null)
                {
                    for (var key : keys)
                    {
                        var fileUpdate = fileUpdates.computeIfAbsent(key.path, path -> new FileUpdates(path, null));
                        fileUpdate.fields.add(key.field);
                    }
                }

                if (fileUpdates.isEmpty())
                {
                    return feature;
                }

                var updates = new ArrayList<GlobalContextUpdate>();
                for (var fileUpdate : fileUpdates.values())
                {
                    var newUpdates = globalContext.getUpdates(
                        new AIContext(aiContext.getProjectId(), fileUpdate.filePath, aiContext.getDocument()),
                        fileUpdate.fileHash, fileUpdate.hashes, fileUpdate.fields, statistics,
                        cancellationToken);

                    if (newUpdates.isEmpty())
                    {
                        continue;
                    }

                    synchronized (updates)
                    {
                        updates.addAll(newUpdates);
                    }

                    var currentIsInitial = isInitial;
                    isInitial = false;
                    feature = feature.thenCompose(i -> {
                        ArrayList<GlobalContextUpdate> latestUpdates;
                        synchronized (updates)
                        {
                            latestUpdates = new ArrayList<>(updates);
                            updates.clear();
                        }

                        if (cancellationToken.isCanceled() || latestUpdates.isEmpty())
                        {
                            return CompletableFuture.completedFuture(true);
                        }

                        return syncUpdates(aiContext, currentIsInitial, latestUpdates, 5, statistics,
                            cancellationToken);
                    });

                    optionalResult =
                        globalContextService
                            .update(aiContext.getProjectId(), updates, 10, statistics, cancellationToken)
                            .get();
                }

                if (cancellationToken.isCanceled())
                {
                    return feature;
                }
            }
        }
        catch (Exception error)
        {
            log.logError(error);
            return CompletableFuture.completedFuture(false);
        }

        return feature;
    }

    private static class FileUpdates
    {
        private final String filePath;
        private final String fileHash;

        public FileUpdates(String filePath, String fileHash)
        {
            this.filePath = filePath;
            this.fileHash = fileHash;
        }

        public final HashSet<String> hashes = new HashSet<>();

        public final HashSet<String> fields = new HashSet<>();
    }
}
