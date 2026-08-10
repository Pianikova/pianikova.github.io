/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.ILog;

/**
 * Small Eclipse-resource-API helpers shared by tools that write project files, extracted from
 * {@link WriteMcpTool} so {@link SvgMcpTool} does not need to duplicate them.
 */
final class WorkspaceFileWriter
{
    private WorkspaceFileWriter()
    {
    }

    static void createParentFolders(IFile file, IProgressMonitor monitor) throws CoreException
    {
        IContainer container = file.getParent();
        if (container instanceof IFolder && !container.exists())
        {
            createFolderRecursive((IFolder)container, monitor);
        }
    }

    static void createFolderRecursive(IFolder folder, IProgressMonitor monitor) throws CoreException
    {
        if (folder == null || folder.exists())
        {
            return;
        }

        IContainer parent = folder.getParent();
        if (parent instanceof IFolder)
        {
            createFolderRecursive((IFolder)parent, monitor);
        }

        if (!folder.exists())
        {
            folder.create(true, true, monitor);
        }
    }

    /**
     * Refreshes the file and its parent folder in the workspace.
     *
     * @param file the file to refresh
     * @param monitor the progress monitor
     * @throws CoreException if refresh fails
     */
    static void refreshResources(IFile file, IProgressMonitor monitor) throws CoreException
    {
        file.refreshLocal(IResource.DEPTH_ZERO, monitor);
        if (file.getParent() != null)
        {
            file.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
        }
    }

    /**
     * Safely refreshes the file and its parent folder. Errors are logged but not thrown.
     *
     * @param file the file to refresh
     * @param monitor the progress monitor
     * @param log where to report a refresh failure
     */
    static void refreshResourcesSafe(IFile file, IProgressMonitor monitor, ILog log)
    {
        try
        {
            refreshResources(file, monitor);
        }
        catch (CoreException error)
        {
            log.logError(error);
        }
    }
}
