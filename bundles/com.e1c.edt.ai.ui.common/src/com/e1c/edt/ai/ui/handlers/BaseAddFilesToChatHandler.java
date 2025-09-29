/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Stack;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IChat;
import com.e1c.edt.ai.ui.IContentReader;
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
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

    public BaseAddFilesToChatHandler()
    {
        BaseActivator.injectMembers(this);
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
        var contents = new Stack<IContentReader>();
        var elements = new LinkedList<>();
        elements.addAll(structuredSelection.toList());
        while (elements.size() > 0)
        {
            var element = elements.removeFirst();
            if (element instanceof IFile)
            {
                var file = ((IFile)element);
                contents.add(new ProjectFileContentReader(file));
                continue;
            }

            if (element instanceof IContainer)
            {
                var container = ((IContainer)element);
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

        if (!contents.isEmpty())
        {
            chat.addFiles(contents);
        }

        return null;
    }

    private class ProjectFileContentReader
        implements IContentReader
    {
        private final IFile file;

        public ProjectFileContentReader(IFile file)
        {
            Preconditions.checkNotNull(file);
            this.file = file;
        }

        @Override
        public ProjectId getProjectId()
        {
            return projectIdProvider.getProjectId(file.getProject());
        }

        @Override
        public String getName()
        {
            return file.getProjectRelativePath().toPortableString();
        }

        @Override
        public Charset getCharset()
        {
            try
            {
                return Charset.forName(file.getCharset());
            }
            catch (CoreException e)
            {
                return Charset.defaultCharset();
            }
        }

        @Override
        public InputStream getInputStream() throws IOException, CoreException
        {
            return fileSystem.getContent(file);
        }
    }
}