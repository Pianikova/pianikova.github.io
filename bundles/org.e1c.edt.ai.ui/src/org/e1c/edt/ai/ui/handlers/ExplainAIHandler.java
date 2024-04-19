/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.Composition;
import org.e1c.edt.ai.ui.IAIContext;
import org.e1c.edt.ai.ui.IChat;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Class Handler of the Explain the Code command.
 *
 * @author Bogdan Sushkov
 */
public class ExplainAIHandler
    extends AbstractHandler
{
    private IAIContext aiContext;
    private final IChat chat;

    public ExplainAIHandler()
    {
        aiContext = Composition.getAIContext();
        chat = Composition.getChat();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        aiContext.create().ifPresent(ctx -> chat.explainCode(ctx.getInput()));
        return null;
    }
}
