/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IIssueFeedbackViewModel;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class BaseFeedbackAIHandler
    extends AbstractHandler
{
    @Inject
    Provider<IIssueFeedbackViewModel> issueFeedbackViewModelProvider;
    @Inject
    ISettings settings;

    public BaseFeedbackAIHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean isEnabled()
    {
        return settings.isEnabled();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        issueFeedbackViewModelProvider.get().getFeedback();
        return null;
    }
}
