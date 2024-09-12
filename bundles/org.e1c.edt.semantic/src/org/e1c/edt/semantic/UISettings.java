/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import org.e1c.edt.ai.IUISettings;

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
    public int getMinRequestDelay()
    {
        return 0;
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
}
