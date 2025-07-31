/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;

import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IUI;
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
    IUI ui;
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
        return codeTools.hasTarget(CodeAction.FIX);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        ui.getLastSourceViewer()
            .flatMap(sourceViewer -> codeTools.createContextForTarget(sourceViewer, CodeAction.FIX))
            .ifPresent(ctx -> {
            if (fixDialog.show() == Window.OK)
            {
                chat.fixCode(ctx, ctx.getText(), fixDialog.getDetails());
            }
        });

        return null;
    }
}
