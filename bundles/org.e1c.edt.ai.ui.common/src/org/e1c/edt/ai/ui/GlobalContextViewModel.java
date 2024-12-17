/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.HashSet;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IGlobalContextRequestFactory;
import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.assistent.IGlobalContextService;
import org.e1c.edt.ai.assistent.model.Completion;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class GlobalContextViewModel implements IGlobalContextViewModel
{
    private final ILog log;
    private final IDispatcher dispatcher;
    private final IGlobalContextRequestFactory globalContextRequestFactory;
    private final IGlobalContextService globalContextService;
    private final Provider<IStatistics> statisticsProvider;
    private final IJson json;

    @Inject
    public GlobalContextViewModel(ILog log, IDispatcher dispatcher,
        IGlobalContextRequestFactory globalContextRequestFactory,
        IGlobalContextService globalContextService, Provider<IStatistics> statisticsProvider, IJson json)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(globalContextRequestFactory);
        Preconditions.checkNotNull(globalContextService);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(json);
        this.log = log;
        this.dispatcher = dispatcher;
        this.globalContextRequestFactory = globalContextRequestFactory;
        this.globalContextService = globalContextService;
        this.statisticsProvider = statisticsProvider;
        this.json = json;
    }

    @Override
    public void registerCompletion(AIContext aiCtx, Completion completion, ICancellationToken cancellationToken)
    {
        if (completion.unknownValues == null && completion.unknownKeys == null && completion.usedKeys == null)
        {
            return;
        }

        log.trace("AI global context is needed " + cancellationToken.toString(), () -> { //$NON-NLS-1$
            return json.serialize(completion);
        });

        dispatcher.dispatchAsync(() -> {
            var cancellationTokenSource = new JobCancellationTokenSource();
            var job = new Job(Messages.CodeCompletionJobName)
            {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    cancellationTokenSource.attachMonitor(monitor);
                    processCompletion(aiCtx, completion, cancellationTokenSource);
                    return cancellationTokenSource.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
                }
            };

            job.schedule();
        });
    }

    private void processCompletion(AIContext aiCtx, Completion completion, ICancellationToken cancellationToken)
    {
        var hashes = new HashSet<String>();
        var fields = new HashSet<String>();
        if (completion.unknownValues != null)
        {
            for (var unknownValue : completion.unknownValues)
            {
                hashes.add(unknownValue.hash);
            }
        }

        if (completion.unknownKeys != null)
        {
            for (var unknownKey : completion.unknownKeys)
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

        if (cancellationToken.isCanceled())
        {
            return;
        }

        try
        {
            globalContextService.update(updates, statistics, cancellationToken).get().ifPresent(result -> {
                if (!cancellationToken.isCanceled() && result.unknownValues != null && !result.unknownValues.isEmpty())
                {
                    var newRequest = new Completion();
                    newRequest.unknownValues = result.unknownValues;
                    registerCompletion(aiCtx, newRequest, cancellationToken);
                }
            });
        }
        catch (Exception error)
        {
            log.logError(error);
        }
    }
}