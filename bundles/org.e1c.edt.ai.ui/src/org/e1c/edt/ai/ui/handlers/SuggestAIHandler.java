/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.Activator;
import org.e1c.edt.ai.ui.ICodeCompletion;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.google.inject.Inject;

public class SuggestAIHandler
    extends AbstractHandler
{
    @Inject
    ICodeCompletion codeCompletion;

    public SuggestAIHandler()
    {
        Activator.injectMembers(this);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        codeCompletion.show(true);
        return null;
    }
}
