/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.expressions.PropertyTester;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.e1c.edt.ai.ui.BaseActivator;
import com.google.inject.Inject;

/**
 * Base property tester for checking if AI is in trace mode
 */
public abstract class BaseTraceModePropertyTester
    extends PropertyTester
{
    private static final String PROPERTY_IS_TRACE_MODE = "isTraceMode"; //$NON-NLS-1$

    @Inject
    protected ISettings settings;

    protected BaseTraceModePropertyTester()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
    {
        if (!PROPERTY_IS_TRACE_MODE.equals(property))
        {
            return false;
        }

        return settings.isEnabled() && settings.getVerbosity() == Verbosity.TRACE;
    }
}
