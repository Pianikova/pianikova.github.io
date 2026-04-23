/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class GlobalContextTracker
    implements IGlobalContextTracker, IStateListener
{
    private final ISettings settings;
    private final IDispatcher dispatcher;
    private final IStateService stateService;
    private final Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider;
    private final ConcurrentHashMap<String, IProjectTrackingWorkflow> projectWorkflows = new ConcurrentHashMap<>();

    @Inject
    public GlobalContextTracker(ISettings settings, IDispatcher dispatcher, IStateService stateService,
        Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider)
    {
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(stateService);
        Preconditions.checkNotNull(projectTrackingWorkflowProvider);
        this.settings = settings;
        this.dispatcher = dispatcher;
        this.stateService = stateService;
        this.projectTrackingWorkflowProvider = projectTrackingWorkflowProvider;

        stateService.addListener(this);
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

        var workflowKey = project.getName();
        var existing = projectWorkflows.get(workflowKey);
        if (existing != null)
        {
            return Optional.of(existing);
        }

        // Cheap allocation via Guice provider; heavy work (initialize + job
        // scheduling) happens OUTSIDE the map section below to avoid holding
        // any lock while doing I/O-bound setup — that was a UI-thread stall
        // source when track(...) was called from the UI thread.
        var candidate = projectTrackingWorkflowProvider.get();
        var previous = projectWorkflows.putIfAbsent(workflowKey, candidate);
        if (previous != null)
        {
            return Optional.of(previous);
        }

        try
        {
            candidate.initialize(project);
            scheduleTracking(workflowKey, candidate, 0);
        }
        catch (RuntimeException error)
        {
            // Roll back so a later caller can retry.
            projectWorkflows.remove(workflowKey, candidate);
            throw error;
        }
        return Optional.of(candidate);
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
            jobCtx -> track(jobCtx, workflowKey, workflow), false, cancellationToken);

        job.setSystem(true);
        job.setPriority(Job.DECORATE);
        job.schedule(delayMs);
    }

    private void track(JobContext jobCtx, String workflowKey, IProjectTrackingWorkflow workflow)
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

    @Override
    public void onServiceStateChange(ServiceState serviceState)
    {
        projectWorkflows.clear();
    }

    @Override
    public void onActionStateChange(com.e1c.edt.ai.ActionState actionState)
    {
        // Do nothing
    }
}
