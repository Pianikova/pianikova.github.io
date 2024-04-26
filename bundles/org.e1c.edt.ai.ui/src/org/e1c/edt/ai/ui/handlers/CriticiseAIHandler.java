/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.Composition;
import org.e1c.edt.ai.ui.IAIContextProvider;
import org.e1c.edt.ai.ui.IChat;
import org.e1c.edt.ai.ui.IUI;
import org.e1c.edt.ai.ui.views.ChatView;
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
    private final IAIContextProvider aiContextProvider;
    private final IChat chat;
    private final IUI ui;

    public CriticiseAIHandler()
    {
        aiContextProvider = Composition.getAIContextProvider();
        chat = Composition.getChat();
        ui = Composition.getUI();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        aiContextProvider.create().ifPresent(ctx -> chat.reviewCode(ctx.getText()));
        ui.showView(ChatView.ID);
        return null;
    }

}
