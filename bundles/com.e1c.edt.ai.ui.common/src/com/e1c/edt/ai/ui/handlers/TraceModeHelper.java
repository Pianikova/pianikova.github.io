/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.e1c.edt.ai.ui.BaseActivator;
import com.google.inject.Inject;

/**
 * Helper class for checking if AI is in trace mode
 */
public class TraceModeHelper
{
    @Inject
    private static ISettings settings;

    static
    {
        BaseActivator.injectMembers(new TraceModeHelper());
    }

    /**
     * Check if AI is in trace mode
     * @return true if AI is enabled and verbosity is TRACE
     */
    public static boolean isTraceMode()
    {
        return settings != null && settings.isEnabled() && settings.getVerbosity() == Verbosity.TRACE;
    }
}
