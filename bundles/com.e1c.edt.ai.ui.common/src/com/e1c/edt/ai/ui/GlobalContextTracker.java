/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class GlobalContextTracker
    implements IGlobalContextTracker
{
    private final IDispatcher dispatcher;
    private final Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider;
    private final IClock clock;
    private final ISettings settings;
    private final Object lockObject = new Object();
    private final HashMap<IProject, IProjectTrackingWorkflow> projectWorkflows = new HashMap<>();

    @Inject
    public GlobalContextTracker(IDispatcher dispatcher,
        Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider, IClock clock, ISettings settings)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(projectTrackingWorkflowProvider);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(settings);
        this.dispatcher = dispatcher;
        this.projectTrackingWorkflowProvider = projectTrackingWorkflowProvider;
        this.clock = clock;
        this.settings = settings;
    }

    @Override
    public void track(IProject project)
    {
        if (!CodeCompletionPolicy.MANUAL.isMeet(settings.getCodeCompletionPolicy()))
        {
            return;
        }

        synchronized (lockObject)
        {
            projectWorkflows.computeIfAbsent(project, k -> projectTrackingWorkflowProvider.get()).initialize(project);
        }
    }

    @Override
    public void track(AIContext aiCtx)
    {
        if (!CodeCompletionPolicy.MANUAL.isMeet(settings.getCodeCompletionPolicy()))
        {
            return;
        }

        synchronized (lockObject)
        {
            var project = aiCtx.getProjectId().project;
            if (project == null)
            {
                return;
            }

            Optional.ofNullable(projectWorkflows.computeIfAbsent(project,
                    k -> projectTrackingWorkflowProvider.get().initialize(project)))
                .ifPresent(workflow -> {
                    workflow.track(aiCtx);
                    var cancellationToken = CancellationTokens.expiresAt(new CancellationTokenSource(), clock,
                        clock.now().plus(Duration.ofMillis(15000)));

                    var job = dispatcher.createJob(Messages.BackgroundJobName,
                        jobCtx -> track(jobCtx, workflow),
                        cancellationToken);

                    job.setPriority(Job.DECORATE);
                    job.schedule();
                });
        }
    }

    private void track(JobContext jobCtx, IProjectTrackingWorkflow workflow)
    {
        Duration delay = Duration.ofSeconds(5);
        try
        {
            delay = workflow.nextState(jobCtx.Monitor, jobCtx.CancellationTokenSource);
        }
        finally
        {
            synchronized (lockObject)
            {
                if (jobCtx.CancellationTokenSource.isCanceled() || !workflow.getProject().exists())
                {
                    return;
                }
            }

            jobCtx.Job.schedule(delay.toMillis());
        }
    }
}
