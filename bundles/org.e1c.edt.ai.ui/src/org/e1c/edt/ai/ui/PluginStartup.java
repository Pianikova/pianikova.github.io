/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

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
public class PluginStartup
    implements IStartup
{
    @Inject
    UI ui;

    public PluginStartup()
    {
        Activator.injectMembers(this);
        var activator = Activator.getDefault();
        activator.trace("Plugin version: " + activator.getPluginVersion().get().toString(), ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void earlyStartup()
    {

        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
        {
            @Override
            public void run()
            {
                var display = Display.getCurrent();
                display.addFilter(SWT.FocusIn, ui);
                display.addFilter(SWT.FocusOut, ui);
            }
        });
    }
}
