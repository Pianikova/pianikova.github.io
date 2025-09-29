/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.google.inject.Inject;

public class BaseAddFileAIHandler
    extends AbstractHandler
{
    @Inject
    IChat chat;

    public BaseAddFileAIHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        chat.addFiles(null);
        return null;
    }
}
