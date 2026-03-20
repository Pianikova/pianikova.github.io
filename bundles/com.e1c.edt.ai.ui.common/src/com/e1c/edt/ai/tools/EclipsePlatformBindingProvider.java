/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.PlatformUI;

import com.google.inject.Singleton;

/**
 * Provides JShell bindings for Eclipse platform services.
 */
@Singleton
public class EclipsePlatformBindingProvider
	implements IJShellBindingProvider
{
    @SuppressWarnings("nls")
    @Override
	public Map<String, Object> getBindings()
	{
		var bindings = new HashMap<String, Object>();
        var workbench = PlatformUI.getWorkbench();
		if (workbench != null)
		{
			bindings.put("workbench", workbench);
            var display = workbench.getDisplay();
			if (display != null)
			{
				bindings.put("display", display);
			}
		}

		return bindings;
	}

    @SuppressWarnings("nls")
    @Override
	public Map<String, JShellBindingDescription> getBindingInfos()
	{
		var infos = new HashMap<String, JShellBindingDescription>();
		infos.put("workbench", new JShellBindingDescription("Eclipse workbench instance",
			"var activeWindow = workbench.getActiveWorkbenchWindow();\nvar activePage = activeWindow.getActivePage();"));
		infos.put("display", new JShellBindingDescription("Eclipse display instance",
			"display.asyncExec(() -> {\n    // UI operations here\n    System.out.println(\"UI Thread: \" + Display.getCurrent());\n});"));
		return infos;
	}

    @Override
    public Collection<Class<?>> getSignificantClasses()
    {
        return List.of(
            org.eclipse.core.runtime.IAdaptable.class, org.eclipse.ui.IWorkbench.class,
            org.eclipse.swt.widgets.Display.class,
            org.eclipse.swt.widgets.Shell.class,
            org.eclipse.ui.IWorkbenchWindow.class,
            org.eclipse.ui.IWorkbenchPage.class,
            org.eclipse.ui.IEditorPart.class,
            org.eclipse.jface.operation.IRunnableContext.class,
            org.eclipse.core.runtime.IAdaptable.class,
            org.eclipse.core.runtime.IProgressMonitor.class,
            org.eclipse.core.commands.common.EventManager.class
        );
    }

    @SuppressWarnings("nls")
    @Override
    public Collection<String> getImports()
    {
        // @formatter:off
		return List.of(
            "import org.eclipse.swt.widgets.*;",
			"import org.eclipse.swt.*;",
			"import org.eclipse.ui.*;",
			"import org.eclipse.jface.operation.*;",
			"import org.eclipse.core.runtime.*;",
			"import org.eclipse.core.commands.common.*;"
		);
		// @formatter:on
    }
}
