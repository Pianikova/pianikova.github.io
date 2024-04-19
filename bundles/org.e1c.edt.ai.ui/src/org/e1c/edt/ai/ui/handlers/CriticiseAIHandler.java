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
 * Class Handler of the Criticise the Code command.
 *
 * @author Bogdan Sushkov
 */
public class CriticiseAIHandler
    extends AbstractHandler
{
    private IAIContext aiContext;
    private final IChat chat;

    public CriticiseAIHandler()
    {
        aiContext = Composition.getAIContext();
        chat = Composition.getChat();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        aiContext.create().ifPresent(ctx -> chat.reviewCode(ctx.getInput()));
        return null;
    }

}
