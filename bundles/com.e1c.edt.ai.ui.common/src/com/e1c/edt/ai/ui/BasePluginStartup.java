/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.IStateService;
import com.google.inject.Inject;

/**
 * This class serves as an activation point for plugins
 * at platform startup. It is instantiated automatically
 * at platform startup, which causes other plugins to be
 * pulled in.
 *
 * @author Bogdan Sushkov
 *
 */
public class BasePluginStartup
    implements IStartup
{
    @Inject
    Set<IInitializable> initializables;
    @Inject
    IStateService accessHolder;
    @Inject
    ILog log;
    @Inject
    IPluginUpdateService pluginUpdateService;
    @Inject
    IDispatcher dispatcher;

    public BasePluginStartup()
    {
        BaseActivator.injectMembers(this);
        var activator = BaseActivator.getDefault();
        var pluginVersion = activator.getPluginVersion();
        var platformVersion = activator.getPlatformVersion();
        activator.trace(
            TracingSources.COMMON,
            platformVersion == null ? "Not 1C:EDT Platform" : "1C:EDT version: " + platformVersion.toString(), //$NON-NLS-1$//$NON-NLS-2$
            () -> ""); //$NON-NLS-1$
        activator.trace(TracingSources.COMMON,
            pluginVersion == null ? "" : "Plugin version: " + pluginVersion.toString(), () -> ""); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

        var updateJob = new Job(Messages.UpdateJobMessage) {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                pluginUpdateService.checkForUpdates();
                schedule(TimeUnit.DAYS.toMillis(1));
                return Status.OK_STATUS;
            }
        };

        updateJob.setPriority(Job.DECORATE);
        updateJob.schedule();
    }

    @Override
    public void earlyStartup()
    {
        accessHolder.startMonitoring(30000, 3000);
        for (var initializable : initializables)
        {
            initializable.initialize();
        }
    }
}
