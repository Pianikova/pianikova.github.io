/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.HashSet;
import java.util.List;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IGlobalContextFactory;
import org.e1c.edt.ai.IGlobalContextRequestFactory;
import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.assistent.IGlobalContextService;
import org.e1c.edt.ai.assistent.model.Completion;
import org.e1c.edt.ai.assistent.model.EntityKey;
import org.e1c.edt.ai.assistent.model.EntityValue;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class GlobalContextManager implements IGlobalContextManager
{
    private final ILog log;
    private final IDispatcher dispatcher;
    private final IGlobalContextRequestFactory globalContextRequestFactory;
    private final IGlobalContextFactory globalContextFactory;
    private final IGlobalContextService globalContextService;
    private final Provider<IStatistics> statisticsProvider;
    private final IJson json;
    private Job currentJob;

    @Inject
    public GlobalContextManager(ILog log, IDispatcher dispatcher,
        IGlobalContextRequestFactory globalContextRequestFactory,
        IGlobalContextFactory globalContextFactory,
        IGlobalContextService globalContextService, Provider<IStatistics> statisticsProvider, IJson json)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(globalContextRequestFactory);
        Preconditions.checkNotNull(globalContextFactory);
        Preconditions.checkNotNull(globalContextService);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(json);
        this.log = log;
        this.dispatcher = dispatcher;
        this.globalContextRequestFactory = globalContextRequestFactory;
        this.globalContextFactory = globalContextFactory;
        this.globalContextService = globalContextService;
        this.statisticsProvider = statisticsProvider;
        this.json = json;
    }

    @Override
    public void warmup(AIContext aiCtx, ICancellationToken cancellationToken)
    {
        var job =
            dispatcher.createJob(Messages.CodeCompletionJobName, ct -> warmupInternal(aiCtx, ct), cancellationToken);
        runJob(job);
    }

    @Override
    public void update(AIContext aiCtx, Completion completion, ICancellationToken cancellationToken)
    {
        updateInternal(aiCtx, completion.unknownValues, completion.unknownKeys, cancellationToken);
    }

    private synchronized void runJob(Job job)
    {
        if (currentJob != null)
        {
            currentJob.cancel();
            currentJob = null;
        }

        currentJob = job;
        job.schedule();
    }

    private void warmupInternal(AIContext aiCtx, ICancellationToken cancellationToken)
    {
        var statistics = statisticsProvider.get();
        var globalContext = globalContextFactory.createGlobalContext(aiCtx, statistics, cancellationToken);
        var updates =
            globalContextRequestFactory.createGlobalContextUpdates(aiCtx, globalContext, statistics, cancellationToken);
        if (cancellationToken.isCanceled() || updates.isEmpty())
        {
            return;
        }

        try
        {
            globalContextService.update(updates, statistics, cancellationToken)
                .get()
                .ifPresent(result -> updateInternal(aiCtx, result.unknownValues, result.unknownKeys, cancellationToken));
        }
        catch (Exception error)
        {
            log.logError(error);
        }
    }

    private void updateInternal(AIContext aiCtx, List<EntityValue> unknownValues, List<EntityKey> unknownKeys,
        ICancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled())
        {
            return;
        }

        var hasUnknownValues = unknownValues != null && !unknownValues.isEmpty();
        var hasUnknownKeys = unknownKeys != null && !unknownKeys.isEmpty();
        if (!hasUnknownValues && !hasUnknownKeys)
        {
            return;
        }

        log.trace("AI global context is needed " + cancellationToken.toString(), () -> { //$NON-NLS-1$
            var trace = new StringBuilder();
            if (hasUnknownValues)
            {
                trace.append("Unknown values:"); //$NON-NLS-1$
                trace.append(System.lineSeparator());
                trace.append(json.serialize(unknownValues));
            }

            if (hasUnknownKeys)
            {
                if (trace.length() > 0)
                {
                    trace.append(System.lineSeparator());
                }

                trace.append("Unknown keys:"); //$NON-NLS-1$
                trace.append(System.lineSeparator());
                trace.append(json.serialize(hasUnknownKeys));
            }

            return trace.toString();
        });

        var job = dispatcher.createJob(Messages.CodeCompletionJobName,
            ct -> processCompletion(aiCtx, unknownValues, unknownKeys, ct), cancellationToken);
        runJob(job);
    }

    private void processCompletion(AIContext aiCtx, List<EntityValue> unknownValues, List<EntityKey> unknownKeys,
        ICancellationToken cancellationToken)
    {
        var hashes = new HashSet<String>();
        var fields = new HashSet<String>();
        if (unknownValues != null)
        {
            for (var unknownValue : unknownValues)
            {
                hashes.add(unknownValue.hash);
            }
        }

        if (unknownKeys != null)
        {
            for (var unknownKey : unknownKeys)
            {
                fields.add(unknownKey.field);
            }
        }

        if (hashes.isEmpty() && fields.isEmpty())
        {
            return;
        }

        var statistics = statisticsProvider.get();
        var updates =
            globalContextRequestFactory.createGlobalContextUpdates(aiCtx, hashes, fields, statistics,
                cancellationToken);

        if (cancellationToken.isCanceled() || updates.isEmpty())
        {
            return;
        }

        try
        {
            globalContextService.update(updates, statistics, cancellationToken)
                .get()
                .ifPresent(
                    result -> updateInternal(aiCtx, result.unknownValues, result.unknownKeys, cancellationToken));
        }
        catch (Exception error)
        {
            log.logError(error);
        }
    }
}