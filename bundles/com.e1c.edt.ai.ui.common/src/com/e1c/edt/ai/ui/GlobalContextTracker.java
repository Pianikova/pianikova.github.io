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
import com.e1c.edt.ai.ISettings;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class GlobalContextTracker
    implements IGlobalContextTracker
{
    private final ISettings settings;
    private final IDispatcher dispatcher;
    private final Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider;
    private final HashMap<String, IProjectTrackingWorkflow> projectWorkflows = new HashMap<>();

    @Inject
    public GlobalContextTracker(ISettings settings, IDispatcher dispatcher,
        Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider)
    {
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(projectTrackingWorkflowProvider);
        this.settings = settings;
        this.dispatcher = dispatcher;
        this.projectTrackingWorkflowProvider = projectTrackingWorkflowProvider;
    }

    @Override
    public Optional<IProjectTrackingWorkflow> track(IProject project)
    {
        if (!settings.isEnabled())
        {
            return Optional.empty();
        }

        if (project == null || !project.exists())
        {
            return Optional.empty();
        }

        synchronized (projectWorkflows)
        {
            var workflowKey = project.getName();
            var workflow = projectWorkflows.computeIfAbsent(workflowKey,
                k -> {
                    var newWorkflow = projectTrackingWorkflowProvider.get();
                    newWorkflow.initialize(project);
                    scheduleTracking(workflowKey, newWorkflow, 0);
                    return newWorkflow;
                });
            return Optional.of(workflow);
        }
    }

    @Override
    public void track(AIContext aiCtx)
    {
        track(aiCtx.getProjectId().project).ifPresent(workflow -> workflow.track(aiCtx));
    }

    private void scheduleTracking(String workflowKey, IProjectTrackingWorkflow workflow, long delayMs)
    {
        var cancellationToken = CancellationTokens.manual(CancellationTokens.NONE, () -> !settings.isEnabled());
        var job = dispatcher.createJob(Messages.BackgroundJobName,
            jobCtx -> track(jobCtx, workflowKey, workflow), cancellationToken);

        job.setSystem(true);
        job.setPriority(Job.DECORATE);
        job.schedule(delayMs);
    }

    private void track(JobContext jobCtx, String workflowKey, IProjectTrackingWorkflow workflow)
    {
        synchronized (projectWorkflows)
        {
            // IDE is closing or AI is disabled
            if (jobCtx.CancellationTokenSource.isCanceled() || !settings.isEnabled())
            {
                projectWorkflows.clear();
                return;
            }

            if (!workflow.getProject().exists())
            {
                projectWorkflows.remove(workflowKey);
                return;
            }
        }

        var delay = Duration.ofSeconds(5);
        try
        {
            delay = workflow.nextState(jobCtx.Monitor, jobCtx.CancellationTokenSource);
        }
        finally
        {
            scheduleTracking(workflowKey, workflow, delay.toMillis());
        }
    }
}
