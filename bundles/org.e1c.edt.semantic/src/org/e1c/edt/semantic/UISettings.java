/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.time.Duration;

import org.e1c.edt.ai.IUISettings;
import org.eclipse.core.runtime.Platform;

public class UISettings
    implements IUISettings
{
    @Override
    public int getTabWidth()
    {
        return 0;
    }

    @Override
    public int getCodeCompletionLinesCount()
    {
        return 0;
    }

    @Override
    public boolean isContinuousCodeCompletion()
    {
        return false;
    }

    @Override
    public Duration getMinRequestDelay()
    {
        return Duration.ZERO;
    }

    @Override
    public Duration getTimeout()
    {
        return Duration.ofSeconds(15);
    }

    @Override
    public String getLineSeparator()
    {
        return "\n"; //$NON-NLS-1$
    }

    @Override
    public boolean sendContext()
    {
        return true;
    }

    @Override
    public String getLanguage()
    {
        return Platform.getNL().equalsIgnoreCase("ru_RU") ? "Russian" : "English"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public String getTheme()
    {
        return "Default"; //$NON-NLS-1$
    }
}
