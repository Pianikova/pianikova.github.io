/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.assistent.model.Completion;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class GlobalContextManager implements IGlobalContextManager
{
    private final IDispatcher dispatcher;
    private final IGlobalContextSync globalContextSync;
    private final IGlobalContextTracker globalContextTracker;
    private final IUISettings settings;
    private Job currentJob;

    @Inject
    public GlobalContextManager(IDispatcher dispatcher, IGlobalContextSync globalContextSync,
        IGlobalContextTracker globalContextTracker, IUISettings settings)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(globalContextSync);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(globalContextTracker);
        Preconditions.checkNotNull(settings);
        this.dispatcher = dispatcher;
        this.globalContextSync = globalContextSync;
        this.globalContextTracker = globalContextTracker;
        this.settings = settings;
    }

    @Override
    public void warmup(AIContext aiCtx, ICancellationToken cancellationToken)
    {
        globalContextTracker.track(aiCtx, false);
        var job =
            dispatcher.createJob(Messages.CodeCompletionBackgroundJobName,
                jobCtx -> globalContextSync.sync(aiCtx, 3, jobCtx.CancellationTokenSource), cancellationToken);
        runJob(job);
    }

    @Override
    public void update(AIContext aiCtx, Completion completion, ICancellationToken cancellationToken)
    {
        var job = dispatcher.createJob(Messages.CodeCompletionJobName, jobCtx -> globalContextSync.sync(aiCtx,
            completion.unknownValues, completion.unknownKeys, 3, jobCtx.CancellationTokenSource), cancellationToken);
        runJob(job);
    }

    @Override
    public void sync(AIContext aiCtx)
    {
        globalContextTracker.track(aiCtx, true);
    }

    private synchronized void runJob(Job job)
    {
        if (currentJob != null)
        {
            currentJob.cancel();
            currentJob = null;
        }

        currentJob = job;
        currentJob.setSystem(!settings.traceMode());
        currentJob.setPriority(Job.DECORATE);
        job.schedule();
    }
}