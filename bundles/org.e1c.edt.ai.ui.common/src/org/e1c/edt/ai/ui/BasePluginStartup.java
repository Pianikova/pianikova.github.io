/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.assistent.IStateService;
import org.eclipse.ui.IStartup;

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
    UI ui;
    @Inject
    IStateService accessHolder;
    @Inject
    ILog log;

    public BasePluginStartup()
    {
        BaseActivator.injectMembers(this);
        var activator = BaseActivator.getDefault();
        var pluginVersion = activator.getPluginVersion();
        var platformVersion = activator.getPlatformVersion();
        activator.trace(
            platformVersion == null ? "Not 1C:EDT Platform" : "1C:EDT version: " + platformVersion.toString(), //$NON-NLS-1$//$NON-NLS-2$
            () -> ""); //$NON-NLS-1$
        activator.trace(pluginVersion == null ? "" : "Plugin version: " + pluginVersion.toString(), () -> ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public void earlyStartup()
    {
        accessHolder.startMonitoring(30000, 3000);
        ui.initialize();
    }
}
