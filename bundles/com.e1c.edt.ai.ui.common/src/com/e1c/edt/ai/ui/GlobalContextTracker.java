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
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ICancellationToken;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class GlobalContextTracker
    implements IGlobalContextTracker
{
    private final IDispatcher dispatcher;
    private final Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider;
    private final Object lockObject = new Object();
    private final HashMap<IProject, IProjectTrackingWorkflow> projectWorkflows = new HashMap<>();

    @Inject
    public GlobalContextTracker(IDispatcher dispatcher,
        Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(projectTrackingWorkflowProvider);
        this.dispatcher = dispatcher;
        this.projectTrackingWorkflowProvider = projectTrackingWorkflowProvider;
    }

    @Override
    public void track(IProject project)
    {
        synchronized (lockObject)
        {
            projectWorkflows.computeIfAbsent(project, k -> projectTrackingWorkflowProvider.get()).initialize(project);
        }
    }

    @Override
    public void track(AIContext aiCtx)
    {
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
                    scheduleTracking(workflow, 0);
                });
        }
    }

    private void scheduleTracking(IProjectTrackingWorkflow workflow, long delayMs)
    {
        var job = dispatcher.createJob(Messages.BackgroundJobName,
            jobCtx -> track(jobCtx, workflow, jobCtx.CancellationTokenSource), CancellationTokens.NONE);

        job.setSystem(true);
        job.setPriority(Job.DECORATE);
        job.schedule(delayMs);
    }

    private void track(JobContext jobCtx, IProjectTrackingWorkflow workflow, ICancellationToken cancellationToken)
    {
        var delay = Duration.ofSeconds(5);
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

            scheduleTracking(workflow, delay.toMillis());
        }
    }
}
