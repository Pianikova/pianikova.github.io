/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.IIssueFeedbackViewModel;
import org.e1c.edt.ai.ui.BaseActivator;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class BaseFeedbackAIHandler
    extends AbstractHandler
{
    @Inject
    Provider<IIssueFeedbackViewModel> issueFeedbackViewModelProvider;

    public BaseFeedbackAIHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        issueFeedbackViewModelProvider.get().getFeedback();
        return null;
    }
}
