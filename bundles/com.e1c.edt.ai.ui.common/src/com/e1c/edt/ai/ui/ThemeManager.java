/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * Manager for detecting Eclipse theme (dark/light).
 *
 * @author AI Assistant
 */
public class ThemeManager
	implements IThemeManager
{
	@Override
	public boolean isDarkTheme()
	{
		return isDarkThemeByPreferences();
	}

	/**
	 * Determines dark theme by checking Eclipse CSS theme preferences.
	 *
	 * @return true if theme is dark, false otherwise
	 */
	private boolean isDarkThemeByPreferences()
	{
		try
		{
			var prefs = InstanceScope.INSTANCE.getNode("org.eclipse.e4.ui.css.swt.theme");
			var themeId = prefs.get("themeid", "");
			return themeId.toLowerCase().contains("dark");
		}
		catch (Exception e)
		{
			// Fallback to color check if preferences fail
			return isDarkThemeByColor();
		}
	}

	/**
	 * Determines dark theme by checking system background color brightness.
	 * This is a fallback method when theme preferences are not available.
	 *
	 * @return true if background is dark (all RGB components < 128), false otherwise
	 */
	private boolean isDarkThemeByColor()
	{
		Color bgColor = Display.getDefault().getSystemColor(org.eclipse.swt.SWT.COLOR_WIDGET_BACKGROUND);
		return bgColor.getRed() < 128 && bgColor.getGreen() < 128 && bgColor.getBlue() < 128;
	}
}
