/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.CancellationTokens;
import org.e1c.edt.ai.ui.AITarget;
import org.e1c.edt.ai.ui.IAIContextProvider;
import org.e1c.edt.ai.ui.IChat;
import org.e1c.edt.ai.ui.IUI;
import org.e1c.edt.ai.ui.BaseActivator;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.google.inject.Inject;

/**
 * Class Handler of the Explain the Code command.
 *
 * @author Bogdan Sushkov
 */
public class BaseExplainAIHandler
    extends AbstractHandler
{
    @Inject
    IAIContextProvider aiContextProvider;
    @Inject
    IChat chat;
    @Inject
    IUI ui;

    public BaseExplainAIHandler()
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
            .flatMap(textWidget -> aiContextProvider.create(
                new AITarget(textWidget, Integer.MAX_VALUE, true), CancellationTokens.NONE))
            .ifPresent(ctx -> chat.explainCode(ctx, ctx.getText()));
        return null;
    }
}
