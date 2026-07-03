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
    // A workflow that returns a delay at or below this keeps running in the same job execution; a longer delay
    // triggers a real reschedule. Keeps the tight INIT/SCAN/HASH/SYNC transitions (~10ms) out of the scheduler.
    private static final Duration ImmediateRescheduleThreshold = Duration.ofMillis(50);
    // Backstop so a workflow with continuous work cannot monopolize the job thread indefinitely.
    private static final int MaxInlineTransitions = 50;
    private final ISettings settings;
    private final IDispatcher dispatcher;
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

        if (project == null || !project.isAccessible())
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
        var delay = Duration.ofSeconds(5);
        try
        {
            // Run consecutive near-immediate transitions within this single job execution instead of
            // rescheduling a fresh Job every ~10ms (which churns the job scheduler). Hand control back with a
            // real reschedule once the workflow asks for a longer pause, hits the inline-step cap, or must
            // stop. Each nextState() does bounded work (scan / hash<=1000 / sync<=1000), so the loop makes
            // forward progress rather than spinning.
            for (var step = 0; step < MaxInlineTransitions; step++)
            {
                // Stop if we have been evicted from the registry (e.g. another workflow replaced us). Without
                // this guard the loop would keep running after eviction and a later track(...) call would
                // start a second, duplicate loop for the same project.
                if (projectWorkflows.get(workflowKey) != workflow)
                {
                    return;
                }

                // IDE is closing or AI is disabled
                if (jobCtx.CancellationTokenSource.isCanceled() || !settings.isEnabled())
                {
                    projectWorkflows.remove(workflowKey, workflow);
                    return;
                }

                if (!workflow.getProject().isAccessible())
                {
                    projectWorkflows.remove(workflowKey, workflow);
                    return;
                }

                delay = workflow.nextState(jobCtx.Monitor, jobCtx.CancellationTokenSource);
                if (delay.compareTo(ImmediateRescheduleThreshold) > 0)
                {
                    break;
                }
            }
        }
        finally
        {
            // Reschedule only while we are still the registered workflow for this project.
            if (projectWorkflows.get(workflowKey) == workflow)
            {
                scheduleTracking(workflowKey, workflow, delay.toMillis());
            }
        }
    }

    @Override
    public void onServiceStateChange(ServiceState serviceState)
    {
        // The server session rotates on these states (ResponseCache invalidates its cache), so the server has
        // lost the context we previously synced. Ask each live workflow to re-sync on its own tracking thread
        // instead of dropping and recreating workflows — the old projectWorkflows.clear() churned background
        // jobs and discarded scan/hash progress on every state change.
        if (serviceState == ServiceState.SETTINGS_CHANGED || serviceState == ServiceState.SESSION_EXPIRED)
        {
            for (var workflow : projectWorkflows.values())
            {
                workflow.requestReset();
            }
        }
    }

    @Override
    public void onActionStateChange(com.e1c.edt.ai.ActionState actionState)
    {
        // Do nothing
    }
}
