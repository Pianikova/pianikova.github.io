/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.semantic;

import java.time.Duration;

import org.eclipse.core.runtime.Platform;

import com.e1c.edt.ai.CodeCompletionPolicy;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.assistent.model.Verbosity;

class UISettings
    implements IUISettings
{
    @Override
    public CodeCompletionPolicy getCodeCompletionPolicy()
    {
        return CodeCompletionPolicy.CREATIVITY;
    }

    @Override
    public void setCodeCompletionPolicy(CodeCompletionPolicy policy)
    {
        //
    }

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
    public boolean sendExtendedContext()
    {
        return true;
    }

    @Override
    public boolean sendGlobalContext()
    {
        return false;
    }

    @Override
    public String getLanguage()
    {
        return Platform.getNL().startsWith("ru_") ? "Russian" : "English"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public String getTheme()
    {
        return "Default"; //$NON-NLS-1$
    }

    @Override
    public Verbosity getVerbosity()
    {
        return Verbosity.DEBUG;
    }
}
