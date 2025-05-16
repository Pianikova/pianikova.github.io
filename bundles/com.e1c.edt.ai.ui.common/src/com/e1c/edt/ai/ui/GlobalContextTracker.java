/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.IProjectProvider;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class GlobalContextTracker
    implements IGlobalContextTracker
{
    private final IDispatcher dispatcher;
    private final IProjectProvider projectProvider;
    private final Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider;
    private final IClock clock;
    private final IUISettings settings;
    private final Object lockObject = new Object();
    private final HashMap<IProject, IProjectTrackingWorkflow> projectWorkflows = new HashMap<>();
    private Job job;

    @Inject
    public GlobalContextTracker(IDispatcher dispatcher, IProjectProvider projectProvider,
        Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider, IClock clock, IUISettings settings)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(projectProvider);
        Preconditions.checkNotNull(projectTrackingWorkflowProvider);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(settings);
        this.dispatcher = dispatcher;
        this.projectProvider = projectProvider;
        this.projectTrackingWorkflowProvider = projectTrackingWorkflowProvider;
        this.clock = clock;
        this.settings = settings;
    }

    @Override
    public void track(AIContext aiCtx)
    {
        if (!CodeCompletionPolicy.FOCUSING.isMeet(settings.getCodeCompletionPolicy()))
        {
            return;
        }

        synchronized (lockObject)
        {
            projectProvider.getProject(aiCtx.getPath())
                .map(project -> projectWorkflows.computeIfAbsent(project,
                    k -> projectTrackingWorkflowProvider.get().initialize(project)))
                .ifPresent(workflow -> {
                    workflow.track(aiCtx);
                    if (job != null && job.getState() != Job.NONE)
                    {
                        return;
                    }

                    var cancellationToken = CancellationTokens.expiresAt(new CancellationTokenSource(), clock,
                        clock.now().plus(Duration.ofMillis(15000)));

                    job = dispatcher.createJob(Messages.CodeCompletionBackgroundJobName,
                        jobCtx -> track(jobCtx, aiCtx, workflow),
                        cancellationToken);

                    job.setPriority(Job.DECORATE);
                    job.schedule();
                });
        }
    }

    private void track(JobContext jobCtx, AIContext aiCtx, IProjectTrackingWorkflow workflow)
    {
        Duration delay = Duration.ofSeconds(5);
        try
        {
            delay = workflow.nextState(jobCtx.Monitor, jobCtx.CancellationTokenSource);
        }
        finally
        {
            if (jobCtx.CancellationTokenSource.isCanceled())
            {
                synchronized (lockObject)
                {
                    job = null;
                    return;
                }
            }

            job.schedule(delay.toMillis());
        }
    }
}
