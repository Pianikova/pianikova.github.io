/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class FileScaner
    implements IFileScaner
{
    private static final HashSet<String> extensions = new HashSet<>();
    private final ILog log;

    static
    {
        extensions.add("bsl"); //$NON-NLS-1$
        extensions.add("mdo"); //$NON-NLS-1$
        extensions.add("form"); //$NON-NLS-1$
    }

    @Inject
    public FileScaner(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    public List<IFile> scan(IProject project)
    {
        var files = new ArrayList<IFile>();
        // The project may be closed concurrently between the caller's accessibility check and
        // the traversal below; in that case accept() throws and we just return what we have.
        if (!project.isAccessible())
        {
            return files;
        }

        try
        {
            project.accept(resource -> {
                if (resource instanceof IFile)
                {
                    var file = (IFile)resource;
                    var pathSegments = file.getProjectRelativePath().segments();
                    if (pathSegments.length <= 2)
                    {
                        return false;
                    }

                    var ext = file.getFileExtension();
                    if (ext == null)
                    {
                        return false;
                    }

                    if (extensions.contains(ext.toLowerCase()))
                    {
                        files.add(file);
                    }

                    return false;
                }

                if (resource instanceof IFolder)
                {
                    var folder = (IFolder)resource;
                    var pathSegments = folder.getProjectRelativePath().segments();
                    if (pathSegments.length == 1)
                    {
                        return "src".equalsIgnoreCase(pathSegments[0]); //$NON-NLS-1$
                    }

                    if (pathSegments.length == 2)
                    {
                        return !"Configuration".equalsIgnoreCase(pathSegments[1]); //$NON-NLS-1$
                    }

                    return pathSegments.length > 2;
                }

                if (resource instanceof IProject)
                {
                    return true;
                }

                return false;
            });
        }
        catch (CoreException error)
        {
            // A project closed mid-traversal is an expected race, not an error worth surfacing.
            if (project.isAccessible())
            {
                log.logError(error);
            }
        }

        return files;
    }
}
