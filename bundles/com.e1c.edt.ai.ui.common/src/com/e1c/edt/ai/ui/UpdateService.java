/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.jobs.Job;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ISettings;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class UpdateService
    implements IInitializable
{
    private final IPluginUpdateService pluginUpdateService;
    private final ISettings settings;
    private final IDispatcher dispatcher;

    @Inject
    public UpdateService(IPluginUpdateService pluginUpdateService, ISettings settings, IDispatcher dispatcher)
    {
        Preconditions.checkNotNull(pluginUpdateService);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(dispatcher);
        this.pluginUpdateService = pluginUpdateService;
        this.settings = settings;
        this.dispatcher = dispatcher;
    }

    @Override
    public void initialize()
    {
        scheduleUpdate(TimeUnit.SECONDS.toMillis(30));
    }

    private void scheduleUpdate(long delayMs)
    {
        var updateJob = dispatcher.createJob(Messages.UpdateJobMessage, jobCtx -> {
            if (settings.isEnabled())
            {
                pluginUpdateService.checkForUpdates(jobCtx.Monitor);
            }

            scheduleUpdate(TimeUnit.DAYS.toMillis(1));
        }, true, CancellationTokens.NONE);

        updateJob.setPriority(Job.DECORATE);
        updateJob.setSystem(true);
        updateJob.schedule(delayMs);
    }
}
