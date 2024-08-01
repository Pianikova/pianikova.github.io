/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.Activator;
import org.e1c.edt.ai.ui.IIssueFeedbackViewModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class FeedbackAIHandler
    extends AbstractHandler
{
    @Inject
    Provider<IIssueFeedbackViewModel> issueFeedbackViewModelProvider;

    public FeedbackAIHandler()
    {
        Activator.injectMembers(this);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        issueFeedbackViewModelProvider.get().getFeedback();
        return null;
    }
}
