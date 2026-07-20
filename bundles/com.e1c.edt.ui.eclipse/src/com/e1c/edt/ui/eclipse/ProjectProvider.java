/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import java.util.Optional;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectProvider;
import com.e1c.edt.ai.TracingSources;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class ProjectProvider
    implements IProjectProvider
{
    private static final String TRACE_TOPIC = "Project resolver"; //$NON-NLS-1$

    private final ILog log;

    @Inject
    ProjectProvider(ILog log)
    {
        this.log = Preconditions.checkNotNull(log);
    }

    @Override
    public Optional<IProject> getProject(String filePath)
    {
        if (filePath == null || filePath.isBlank())
        {
            traceFailure(filePath);
            return Optional.empty();
        }

        var root = ResourcesPlugin.getWorkspace().getRoot();
        var path = Path.fromOSString(filePath);
        IResource resource = path.isAbsolute() ? root.getFileForLocation(path) : root.findMember(path);
        if (resource != null)
        {
            return trace(Optional.of(resource.getProject()).filter(IProject::isAccessible), filePath);
        }

        if (path.isAbsolute())
        {
            IContainer[] containers = root.findContainersForLocationURI(path.toFile().toURI());
            if (containers.length > 0)
            {
                return trace(Optional.of(containers[0].getProject()).filter(IProject::isAccessible), filePath);
            }

            IProject bestMatch = null;
            var bestLength = -1;
            for (var project : root.getProjects())
            {
                var location = project.getLocation();
                if (location != null && location.isPrefixOf(path) && location.segmentCount() > bestLength)
                {
                    bestMatch = project;
                    bestLength = location.segmentCount();
                }
            }

            return trace(Optional.ofNullable(bestMatch).filter(IProject::isAccessible), filePath);
        }

        traceFailure(filePath);
        return Optional.empty();
    }

    private Optional<IProject> trace(Optional<IProject> project, String filePath)
    {
        project.ifPresentOrElse(value -> log.trace(TracingSources.COMMON, TRACE_TOPIC,
            () -> "Project resolved: source=path, project=" + value.getName() + ", path=" + filePath), //$NON-NLS-1$ //$NON-NLS-2$
            () -> traceFailure(filePath));
        return project;
    }

    private void traceFailure(String filePath)
    {
        log.trace(TracingSources.COMMON, TRACE_TOPIC,
            () -> "Project resolution failed: source=path, path=" + filePath); //$NON-NLS-1$
    }
}
