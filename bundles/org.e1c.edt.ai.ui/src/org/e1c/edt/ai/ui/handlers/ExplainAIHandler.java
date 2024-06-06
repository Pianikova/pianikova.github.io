/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.CancellationToken;
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
 * Class Handler of the Explain the Code command.
 *
 * @author Bogdan Sushkov
 */
public class ExplainAIHandler
    extends AbstractHandler
{
    @Inject
    IAIContextProvider<Void> aiContextProvider;
    @Inject
    IChat chat;
    @Inject
    IUI ui;

    public ExplainAIHandler()
    {
        Activator.injectMembers(this);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        aiContextProvider.create(null, CancellationToken.NONE).ifPresent(ctx -> chat.explainCode(ctx.getText()));
        ui.showView(ChatView.ID);
        return null;
    }
}
