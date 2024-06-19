/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.CancellationTokens;
import org.e1c.edt.ai.ui.AITarget;
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
 * Class Handler of the fix the Code command.
 *
 * @author George Suaridze
 */
public class FixCodeAIHandler
    extends AbstractHandler
{
    @Inject
    IAIContextProvider<AITarget> aiContextProvider;
    @Inject
    IChat chat;
    @Inject
    IUI ui;

    public FixCodeAIHandler()
    {
        Activator.injectMembers(this);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        ui.getTextWidget()
            .flatMap(textWidget -> aiContextProvider.create(new AITarget(textWidget, Integer.MAX_VALUE),
                CancellationTokens.NONE))
            .ifPresent(ctx -> chat.fixCode(ctx.getText()));
        ui.showView(ChatView.ID);
        return null;
    }
}
