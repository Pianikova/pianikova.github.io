/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IGlobalContextManager;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.model.Completion;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class GlobalContextManager implements IGlobalContextManager
{
    private final ILog log;
    private final IDispatcher dispatcher;
    private final IGlobalContextSync globalContextSync;
    private final IGlobalContextTracker globalContextTracker;
    private Job currentJob;

    @Inject
    public GlobalContextManager(ILog log, IDispatcher dispatcher, IGlobalContextSync globalContextSync,
        IGlobalContextTracker globalContextTracker)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(globalContextSync);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(globalContextTracker);
        this.log = log;
        this.dispatcher = dispatcher;
        this.globalContextSync = globalContextSync;
        this.globalContextTracker = globalContextTracker;
    }

    @Override
    public void update(AIContext aiCtx, ICancellationToken cancellationToken)
    {
        var job =
            dispatcher.createJob(Messages.BackgroundJobName,
                jobCtx -> {
                    try
                    {
                        var syncTask = globalContextSync.sync(aiCtx, 5, jobCtx.CancellationTokenSource);
                        CancellationTokenSource.attach(jobCtx.CancellationTokenSource, () -> syncTask.cancel(true));
                        syncTask.get();
                        globalContextTracker.track(aiCtx);
                    }
                    catch (ExecutionException error)
                    {
                        log.trace(TracingSources.SYNC, "GlobalContextManager", //$NON-NLS-1$
                            () -> "Error updating global context: " + error); //$NON-NLS-1$
                    }
                    catch (Exception error)
                    {
                        log.logError(error);
                    }
                }, cancellationToken);

        runJob(job);
    }

    @Override
    public void update(AIContext aiCtx, Completion completion, ICancellationToken cancellationToken)
    {
        if (completion.unknownValues == null || completion.unknownValues.isEmpty())
        {
            return;
        }

        var job = dispatcher.createJob(Messages.CodeCompletionJobName, jobCtx -> {
            try
            {
                var syncTask =
                    globalContextSync.syncUnknown(aiCtx, completion.unknownValues, 5, jobCtx.CancellationTokenSource);
                CancellationTokenSource.attach(jobCtx.CancellationTokenSource, () -> syncTask.cancel(true));
                syncTask.get();
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        }, cancellationToken);
        runJob(job);
    }

    private synchronized void runJob(Job job)
    {
        if (currentJob != null)
        {
            currentJob.cancel();
            currentJob = null;
        }

        currentJob = job;
        currentJob.setSystem(true);
        currentJob.setPriority(Job.DECORATE);
        job.schedule();
    }
}