/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.Composition;
import org.e1c.edt.ai.ui.IAIContext;
import org.e1c.edt.ai.ui.IChat;
import org.e1c.edt.ai.ui.IUI;
import org.e1c.edt.ai.ui.views.ChatView;
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
    private final IAIContext aiContext;
    private final IChat chat;
    private final IUI ui;

    public ExplainAIHandler()
    {
        aiContext = Composition.getAIContext();
        chat = Composition.getChat();
        ui = Composition.getUI();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        aiContext.create().ifPresent(ctx -> chat.explainCode(ctx.getText()));
        ui.showView(ChatView.ID);
        return null;
    }
}
