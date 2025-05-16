/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IUI;
import com.google.inject.Inject;

/**
 * Class Handler of the Criticise the Code command.
 *
 * @author Bogdan Sushkov
 */
public class BaseCriticiseAIHandler
    extends AbstractHandler
{
    @Inject
    IUI ui;
    @Inject
    IChat chat;
    @Inject
    ICodeTools codeTools;

    public BaseCriticiseAIHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean isEnabled()
    {
        return codeTools.hasTarget();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        ui.getLastSourceViewer()
            .flatMap(sourceViewer -> codeTools.createContextForTarget(sourceViewer))
            .ifPresent(ctx -> chat.reviewCode(ctx, ctx.getText()));
        return null;
    }
}
