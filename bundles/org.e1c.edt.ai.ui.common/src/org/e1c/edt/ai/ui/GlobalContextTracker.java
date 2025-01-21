/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationTokenSource;
import org.e1c.edt.ai.CancellationTokens;
import org.e1c.edt.ai.IClock;
import org.e1c.edt.ai.IProjectProvider;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IUISettings;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class GlobalContextTracker
    implements IGlobalContextTracker, AutoCloseable
{
    private final IDispatcher dispatcher;
    private final IProjectProvider projectProvider;
    private final Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider;
    private final IClock clock;
    private final ISettingsStore settingsStore;
    private final IUISettings settings;
    private final Object lockObject = new Object();
    private final HashMap<IProject, IProjectTrackingWorkflow> projectWorkflows = new HashMap<>();
    private final State state;
    private Job job;

    @Inject
    public GlobalContextTracker(IDispatcher dispatcher, IProjectProvider projectProvider,
        Provider<IProjectTrackingWorkflow> projectTrackingWorkflowProvider, IClock clock, ISettingsStore settingsStore,
        IUISettings settings)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(projectProvider);
        Preconditions.checkNotNull(projectTrackingWorkflowProvider);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(settingsStore);
        Preconditions.checkNotNull(settings);
        this.dispatcher = dispatcher;
        this.projectProvider = projectProvider;
        this.projectTrackingWorkflowProvider = projectTrackingWorkflowProvider;
        this.clock = clock;
        this.settingsStore = settingsStore;
        this.settings = settings;
        state = settingsStore.getValue(ISettingsStore.GLOBAL_CONTEXT, State.class).orElseGet(() -> {
            var result = new State();
            result.hashes = new HashSet<>();
            return result;
        });
    }

    @Override
    public void track(AIContext aiCtx, boolean forcible)
    {
        synchronized (lockObject)
        {
            projectProvider.getProject(aiCtx.getPath()).ifPresent(project -> {
                var workflow =
                    projectWorkflows.computeIfAbsent(project,
                        k -> projectTrackingWorkflowProvider.get().initialize(project, state.hashes));

                if (forcible)
                {
                    workflow.markAsDirty(aiCtx.getPath());
                }

                if (job != null && job.getState() != Job.NONE)
                {
                    return;
                }

                var cancellationToken = CancellationTokens.expiresAt(new CancellationTokenSource(), clock,
                    clock.now().plus(Duration.ofMillis(15000)));

                job = dispatcher.createJob(Messages.CodeCompletionBackgroundJobName,
                    jobCtx -> track(jobCtx, workflow),
                    cancellationToken);

                job.setSystem(!settings.traceMode());
                job.setPriority(Job.DECORATE);
                job.schedule();
            });
        }
    }

    @Override
    public void close() throws Exception
    {
        synchronized (lockObject)
        {
            state.hashes.clear();
            for (var workflow : projectWorkflows.values())
            {
                state.hashes.addAll(workflow.getHashes());
            }
        }

        settingsStore.setValue(ISettingsStore.GLOBAL_CONTEXT, state);
    }

    private void track(JobContext jobCtx, IProjectTrackingWorkflow workflow)    {
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

    private static class State
    {
        public HashSet<String> hashes = new HashSet<>();
    }
}
