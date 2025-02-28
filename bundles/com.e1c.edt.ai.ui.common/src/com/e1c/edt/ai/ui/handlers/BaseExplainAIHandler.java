/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
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
    IChat chat;
    @Inject
    ICodeTools codeTools;

    public BaseExplainAIHandler()
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
        codeTools.createContextForTarget().ifPresent(ctx -> chat.explainCode(ctx, ctx.getText()));
        return null;
    }
}
