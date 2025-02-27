/*
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.ui.BaseActivator;
import org.e1c.edt.ai.ui.IChat;
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
    IChat chat;
    @Inject
    ICodeTools codeTools;
    @Inject
    IFixDialog fixDialog;

    public BaseFixCodeAIHandler()
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
        codeTools.createContextForTarget().ifPresent(ctx -> {
            if (fixDialog.show() == Window.OK)
            {
                chat.fixCode(ctx, ctx.getText(), fixDialog.getDetails());
            }
        });

        return null;
    }
}
