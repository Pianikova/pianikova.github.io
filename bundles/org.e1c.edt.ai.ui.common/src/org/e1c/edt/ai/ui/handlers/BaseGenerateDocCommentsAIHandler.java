/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.BaseActivator;
import org.e1c.edt.ai.ui.IChat;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

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
        codeTools.getTargetMethod().ifPresent(targetMethod -> {
            codeTools.selectMethodComment(targetMethod);
            chat.generateDocComments(targetMethod.ctx, targetMethod.methodText);
        });
        return null;
    }
}
