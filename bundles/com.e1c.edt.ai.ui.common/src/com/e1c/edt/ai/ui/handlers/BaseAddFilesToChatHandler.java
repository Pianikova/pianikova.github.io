/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.commands.ExpressionContext;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.e1c.edt.ai.IFiles;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IContentSourceProvider;
import com.e1c.edt.ai.ui.IFileContent;
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.inject.Inject;

public class BaseAddFilesToChatHandler
    extends AbstractHandler
{
    @Inject
    IChat chat;
    @Inject
    IProjectIdProvider projectIdProvider;
    @Inject
    IFileSystem fileSystem;
    @Inject
    ISettings settings;
    @Inject
    IContentSourceProvider contentSourceProvider;
    @Inject
    IFiles files;

    public BaseAddFilesToChatHandler()
    {
        BaseActivator.injectMembers(this);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void setEnabled(Object evaluationContext)
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
                setBaseEnabled(!getContents((List)elements).isEmpty());
                return;
            }
        }

        setBaseEnabled(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object execute(ExecutionEvent event)
    {
        var selection = HandlerUtil.getCurrentSelection(event);
        if (selection == null || !(selection instanceof IStructuredSelection))
        {
            return null;
        }

        var structuredSelection = (IStructuredSelection)HandlerUtil.getCurrentSelection(event);
        var contents = getContents(structuredSelection.toList());
        if (!contents.isEmpty())
        {
            chat.addFiles(contents);
        }

        return null;
    }

    private List<IFileContent> getContents(List<Object> targets)
    {
        var contents = new Stack<IFileContent>();
        if (targets == null || targets.isEmpty())
        {
            return contents;
        }

        var elements = new LinkedList<>();
        elements.addAll(targets);
        while (elements.size() > 0)
        {
            var element = elements.removeFirst();
            if (element instanceof EObject)
            {
                var file = files.getCodeFile((EObject)element);
                if (file.isPresent())
                {
                    var optionalContent = contentSourceProvider.getFileContent(file.get());
                    if (optionalContent.isPresent())
                    {
                        contents.add(optionalContent.get());
                    }

                    continue;
                }
            }

            if (element instanceof IFile)
            {
                var file = ((IFile)element);
                if (file.isHidden() || file.isVirtual() || !file.exists())
                {
                    continue;
                }

                var optionalContent = contentSourceProvider.getFileContent(file);
                if (optionalContent.isPresent())
                {
                    contents.add(optionalContent.get());
                }

                continue;
            }

            if (element instanceof IContainer)
            {
                var container = ((IContainer)element);
                if (container.isHidden() || container.isVirtual() || container instanceof IProject)
                {
                    continue;
                }

                try
                {
                    for (var member : container.members())
                    {
                        elements.add(member);
                    }
                }
                catch (CoreException e)
                {
                    //
                }

                continue;
            }
        }

        return contents;
    }
}