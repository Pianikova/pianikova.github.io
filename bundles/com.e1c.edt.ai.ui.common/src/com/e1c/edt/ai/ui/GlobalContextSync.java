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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.Fields;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IGlobalContext;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.IGlobalContextService;
import com.e1c.edt.ai.assistent.model.EntityValue;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdateResponse;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class GlobalContextSync implements IGlobalContextSync
{
    private final ILog log;
    private final ISettings settings;
    private final Provider<IStatistics> statisticsProvider;
    private final IJson json;
    private final IGlobalContext globalContext;
    private final IGlobalContextService globalContextService;

    @Inject
    public GlobalContextSync(ILog log, ISettings settings, Provider<IStatistics> statisticsProvider, IJson json,
        IGlobalContext globalContext, IGlobalContextService globalContextService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(globalContext);
        Preconditions.checkNotNull(globalContextService);
        this.log = log;
        this.settings = settings;
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
            var updates = globalContext.getUpdates(aiContext, statistics, cancellationToken);
            return syncUpdates(aiContext, updates, maxDept, statistics, cancellationToken);
        }
        catch (Exception error)
        {
            log.logError(error);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public CompletableFuture<Boolean> syncUpdates(AIContext aiContext, List<GlobalContextUpdate> updates,
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

                    return syncUnknown(aiContext, result.unknownValues, maxDept,
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
    public CompletableFuture<Boolean> syncUnknown(AIContext aiContext, List<EntityValue> unknownValues, int maxDept,
        ICancellationToken cancellationToken)
    {
        if (unknownValues == null || unknownValues.isEmpty())
        {
            return CompletableFuture.completedFuture(true);
        }

        try
        {
            if (cancellationToken.isCanceled())
            {
                return CompletableFuture.completedFuture(false);
            }

            var statistics = statisticsProvider.get();
            var response = new GlobalContextUpdateResponse();
            response.unknownValues = unknownValues;
            var optionalResult = Optional.of(response);
            while (maxDept-- > 0 && optionalResult.isPresent())
            {
                var result = optionalResult.get();
                if (result.isEmpty())
                {
                    return CompletableFuture.completedFuture(true);
                }

                var vals = result.unknownValues;
                var hasUnknownValues = vals != null && !vals.isEmpty();
                log.trace(TracingSources.SYNC, "AI global context is needed " + cancellationToken.toString(), () -> { //$NON-NLS-1$
                    var trace = new StringBuilder();
                    if (hasUnknownValues)
                    {
                        trace.append("Unknown values:"); //$NON-NLS-1$
                        trace.append(System.lineSeparator());
                        trace.append(json.serialize(vals));
                    }

                    return trace.toString();
                });

                var fileUpdates = new HashMap<String, FileUpdates>();
                if (vals != null)
                {
                    for (var val : vals)
                    {
                        String fileHash = null;
                        var field = val.field;
                        if (Fields.LOCAL_FUNCTIONS.equalsIgnoreCase(field) || Fields.FORM.equalsIgnoreCase(field)
                            || Fields.META.equalsIgnoreCase(field))
                        {
                            fileHash = val.hash;
                        }

                        var hash = fileHash;
                        var fileUpdate = fileUpdates.computeIfAbsent(val.path, path -> new FileUpdates(path, hash));
                        fileUpdate.hashes.add(val.hash);
                        fileUpdate.fields.add(val.field);
                    }
                }

                if (fileUpdates.isEmpty())
                {
                    return CompletableFuture.completedFuture(true);
                }

                // Collect this round's updates into a fresh local list (no shared mutable state, no racing
                // async callback clearing it under us), then send them in a single call. The server's unknown
                // values feed the next loop iteration, bounded by maxDept.
                var updates = new ArrayList<GlobalContextUpdate>();
                for (var fileUpdate : fileUpdates.values())
                {
                    updates.addAll(globalContext.getUpdates(
                        new AIContext(aiContext.getProjectId(), fileUpdate.filePath, aiContext.getDocument()),
                        fileUpdate.fileHash, fileUpdate.hashes, fileUpdate.fields, statistics,
                        cancellationToken));
                }

                if (updates.isEmpty())
                {
                    return CompletableFuture.completedFuture(true);
                }

                if (cancellationToken.isCanceled())
                {
                    return CompletableFuture.completedFuture(false);
                }

                try
                {
                    var timeout = settings.getTimeout();
                    optionalResult = globalContextService
                        .update(aiContext.getProjectId(), updates, 10, statistics, cancellationToken)
                        .get(timeout.toNanos(), TimeUnit.NANOSECONDS);
                }
                catch (TimeoutException error)
                {
                    log.warning(TracingSources.SYNC,
                        () -> "Global context update timed out after " //$NON-NLS-1$
                            + settings.getTimeout());
                    return CompletableFuture.completedFuture(false);
                }
            }
        }
        catch (Exception error)
        {
            log.logError(error);
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(true);
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
