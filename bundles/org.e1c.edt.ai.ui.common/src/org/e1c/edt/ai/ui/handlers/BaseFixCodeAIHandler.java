/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.CancellationTokens;
import org.e1c.edt.ai.ui.AITarget;
import org.e1c.edt.ai.ui.BaseChatView;
import org.e1c.edt.ai.ui.IAIContextProvider;
import org.e1c.edt.ai.ui.IChat;
import org.e1c.edt.ai.ui.IUI;
import org.e1c.edt.ai.ui.BaseActivator;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;

import com.google.inject.Inject;

/**
 * Class Handler of the fix the Code command.
 *
 * @author George Suaridze
 */
public class BaseFixCodeAIHandler
    extends AbstractHandler
{
    @Inject
    IAIContextProvider aiContextProvider;
    @Inject
    IChat chat;
    @Inject
    IUI ui;
    @Inject
    IFixDialog fixDialog;

    public BaseFixCodeAIHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean isEnabled()
    {
        return ui.getTextWidget().map(textWidget -> !textWidget.getSelectionText().isBlank()).orElse(false);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        ui.getTextWidget()
            .flatMap(textWidget -> aiContextProvider.create(new AITarget(textWidget, Integer.MAX_VALUE, true),
                CancellationTokens.NONE))
            .ifPresent(ctx -> {
                if (fixDialog.show() == Window.OK)
                {
                    chat.fixCode(ctx, ctx.getText(), fixDialog.getDetails());
                }
            });
        ui.showView(BaseChatView.ID);
        return null;
    }
}
