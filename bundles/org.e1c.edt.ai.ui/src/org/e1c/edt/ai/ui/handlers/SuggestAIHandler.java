/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.Composition;
import org.e1c.edt.ai.ui.ICodeCompletion;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class SuggestAIHandler
    extends AbstractHandler
{
    private final ICodeCompletion codeCompletion;

    public SuggestAIHandler()
    {
        this.codeCompletion = Composition.getCodeCompletion();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        codeCompletion.show(true);
        return null;
    }
}
