/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EObject;

import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.IFiles;
import com.google.inject.Inject;

public class ChatFileSelectionResolver
    implements IChatFileSelectionResolver
{
    @Inject
    IContentSourceProvider contentSourceProvider;
    @Inject
    IFiles files;

    @Override
    public List<IFileDocument> resolve(List<?> targets)
    {
        var contents = new Stack<IFileDocument>();
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
                var eObject = (EObject)element;
                var file = files.getCodeFile(eObject);
                if (file.isPresent())
                {
                    var optionalContent = contentSourceProvider.getFileDocument(file.get());
                    if (optionalContent.isPresent())
                    {
                        contents.add(optionalContent.get());
                    }

                    continue;
                }

                // Any other metadata object (Document, Catalog, Form, Template, ...): add every
                // file from the object's own folder by recursing into it as a container.
                var folder = files.getObjectFolder(eObject);
                if (folder.isPresent())
                {
                    elements.add(folder.get());
                }

                continue;
            }

            if (element instanceof IFile)
            {
                var file = ((IFile)element);
                if (file.isHidden() || file.isVirtual() || !file.exists())
                {
                    continue;
                }

                var optionalContent = contentSourceProvider.getFileDocument(file);
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

    @Override
    public boolean canResolve(List<?> targets)
    {
        if (targets == null)
        {
            return false;
        }

        for (var element : targets)
        {
            if (element instanceof IFile)
            {
                var file = (IFile)element;
                if (!file.isHidden() && !file.isVirtual() && file.exists())
                {
                    return true;
                }
            }
            else if (element instanceof IContainer)
            {
                var container = (IContainer)element;
                if (!container.isHidden() && !container.isVirtual() && !(container instanceof IProject))
                {
                    return true;
                }
            }
            else if (element instanceof EObject)
            {
                var eObject = (EObject)element;
                if (files.getCodeFile(eObject).isPresent() || files.getObjectFolder(eObject).isPresent())
                {
                    return true;
                }
            }
        }

        return false;
    }
}
