/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ITraceScenario;
import com.e1c.edt.ai.TraceScenarioType;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.google.inject.Inject;

public class BaseServerErrorAIHandler
    extends AbstractHandler
{
    @Inject
    IChat chat;
    @Inject
    ISettings settings;
    @Inject
    ITraceScenario traceScenario;

    public BaseServerErrorAIHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean isEnabled()
    {
        return settings.isEnabled() && settings.getVerbosity() == Verbosity.TRACE;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        traceScenario.activate(TraceScenarioType.SERVER_ERROR);
        return null;
    }
}
