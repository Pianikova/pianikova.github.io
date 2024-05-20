/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.Activator;
import org.e1c.edt.ai.ui.ChatView;
import org.e1c.edt.ai.ui.IAIContextProvider;
import org.e1c.edt.ai.ui.IChat;
import org.e1c.edt.ai.ui.IUI;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.google.inject.Inject;

/**
 * Class Handler of the Criticise the Code command.
 *
 * @author Bogdan Sushkov
 */
public class CriticiseAIHandler
    extends AbstractHandler
{
    @Inject
    IAIContextProvider aiContextProvider;
    @Inject
    IChat chat;
    @Inject
    IUI ui;

    public CriticiseAIHandler()
    {
        Activator.injectMembers(this);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        aiContextProvider.create().ifPresent(ctx -> chat.reviewCode(ctx.getText()));
        ui.showView(ChatView.ID);
        return null;
    }
}
