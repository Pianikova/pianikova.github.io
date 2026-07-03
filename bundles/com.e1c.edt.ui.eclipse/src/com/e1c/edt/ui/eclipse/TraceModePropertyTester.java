/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import org.eclipse.core.expressions.PropertyTester;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.e1c.edt.ai.ui.BaseActivator;
import com.google.inject.Inject;

public class TraceModePropertyTester
    extends PropertyTester
{
    @Inject
    ISettings settings;

    public TraceModePropertyTester()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
    {
        if (!isLogLevel(Verbosity.TRACE))
        {
            return false;
        }
        return true;
    }

    private boolean isLogLevel(Verbosity verbosity)
    {
        return settings.getVerbosity().getLevel() >= verbosity.getLevel();
    }
}
