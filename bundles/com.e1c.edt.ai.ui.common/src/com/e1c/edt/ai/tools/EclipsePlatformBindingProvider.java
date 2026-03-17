/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
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

		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench != null)
		{
			bindings.put("workbench", workbench);

			Display display = workbench.getDisplay();
			if (display != null)
			{
				bindings.put("display", display);
			}
		}

		return bindings;
	}

    @SuppressWarnings("nls")
    @Override
	public Map<String, String> getBindingDescriptions()
	{
		var descriptions = new HashMap<String, String>();
		descriptions.put("workbench", "Eclipse workbench instance");
		descriptions.put("display", "Eclipse display instance");
		return descriptions;
	}
}
