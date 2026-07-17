/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.e4.core.commands.ExpressionContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.inject.Inject;

public class BaseAddFilesToChatHandler
    extends AbstractHandler
{
    @Inject
    IChat chat;
    @Inject
    IFileSystem fileSystem;
    @Inject
    ISettings settings;
    @Inject
    IChatFileSelectionResolver selectionResolver;
    @Inject
    ILog log;

    public BaseAddFilesToChatHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void setEnabled(Object evaluationContext)
    {
        try
        {
            if (!settings.isEnabled())
            {
                setBaseEnabled(false);
                return;
            }

            if (evaluationContext instanceof ExpressionContext)
            {
                var expressionContext = (ExpressionContext)evaluationContext;
                var elements = expressionContext.getDefaultVariable();
                if (elements instanceof List)
                {
                    setBaseEnabled(!selectionResolver.resolve((List)elements).isEmpty());
                    return;
                }
            }

            setBaseEnabled(false);
        }
        catch (Exception e)
        {
            if (log != null)
            {
                log.logError(e);
            }
            setBaseEnabled(false);
        }
    }

    @Override
    public Object execute(ExecutionEvent event)
    {
        try
        {
            var selection = HandlerUtil.getCurrentSelection(event);
            if (selection == null || !(selection instanceof IStructuredSelection))
            {
                return null;
            }

            var structuredSelection = (IStructuredSelection)HandlerUtil.getCurrentSelection(event);
            var contents = selectionResolver.resolve(structuredSelection.toList());
            if (!contents.isEmpty())
            {
                chat.addFiles(contents);
            }
        }
        catch (Exception e)
        {
            if (log != null)
            {
                log.logError(e);
            }
        }

        return null;
    }
}
