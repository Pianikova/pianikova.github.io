/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.startup;

import org.e1c.edt.ai.ui.Composition;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
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
    private final IPartListener2 partListener;
    private final ISelectionListener selectionListener;

    public PluginStartup()
    {
        partListener = Composition.getPartListener();
        selectionListener = Composition.getSelectionListener();
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
