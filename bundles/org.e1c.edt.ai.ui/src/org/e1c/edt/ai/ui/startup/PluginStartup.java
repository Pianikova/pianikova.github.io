/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.startup;

import org.e1c.edt.ai.ui.AIPartListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;

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
    @Override
    public void earlyStartup()
    {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
        {
            @SuppressWarnings("restriction")
            @Override
            public void run()
            {
                Workbench.getInstance()
                    .getActiveWorkbenchWindow()
                    .getPartService()
                    .addPartListener(new AIPartListener());
            }
        });
    }
}
