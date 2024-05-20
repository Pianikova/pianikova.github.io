/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;

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
    IPartListener2 partListener;
    @Inject
    ISelectionListener selectionListener;

    public PluginStartup()
    {
        Activator.injectMembers(this);
    }

    @Override
    public void earlyStartup()
    {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
        {
            @SuppressWarnings("restriction")
            @Override
            public void run()
            {
                var partService = Workbench.getInstance()
                    .getActiveWorkbenchWindow()
                    .getPartService();

                partService.getActivePartReference().getPage().addPostSelectionListener(selectionListener);
                partService.addPartListener(partListener);
            }
        });
    }
}
