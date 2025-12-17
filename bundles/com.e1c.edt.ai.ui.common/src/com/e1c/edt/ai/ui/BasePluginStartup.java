/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Set;

import org.eclipse.ui.IStartup;

import com.e1c.edt.ai.TracingSources;
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
    }

    @Override
    public void earlyStartup()
    {
        for (var initializable : initializables)
        {
            initializable.initialize();
        }
    }
}
