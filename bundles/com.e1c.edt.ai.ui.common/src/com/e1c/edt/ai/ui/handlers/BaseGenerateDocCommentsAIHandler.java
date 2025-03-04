/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.google.inject.Inject;

/**
 * Class Handler of the fix the Code command.
 *
 * @author George Suaridze
 */
public class BaseGenerateDocCommentsAIHandler
    extends AbstractHandler
{
    @Inject
    IChat chat;
    @Inject
    ICodeTools codeTools;

    public BaseGenerateDocCommentsAIHandler()
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
        var optionalTargetMethod = codeTools.getTargetMethod();
        if (optionalTargetMethod.isPresent())
        {
            var targetMethod = optionalTargetMethod.get();
            codeTools.selectMethodComment(targetMethod);
            chat.generateDocComments(targetMethod.ctx, targetMethod.methodText);
        }
        else
        {
            codeTools.createContextForTarget().ifPresent(ctx -> chat.generateDocComments(ctx, ctx.getText()));
        }

        return null;
    }
}
