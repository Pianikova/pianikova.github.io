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

import com.e1c.edt.ai.IProjectProvider;

class ProjectProvider
    implements IProjectProvider
{
    @Override
    public Optional<IProject> getProject(String filePath)
    {
        if (filePath == null || filePath.isBlank())
        {
            return Optional.empty();
        }

        var root = ResourcesPlugin.getWorkspace().getRoot();
        var path = Path.fromOSString(filePath);
        IResource resource = path.isAbsolute() ? root.getFileForLocation(path) : root.findMember(path);
        if (resource != null)
        {
            return Optional.of(resource.getProject()).filter(IProject::isAccessible);
        }

        if (path.isAbsolute())
        {
            IContainer[] containers = root.findContainersForLocationURI(path.toFile().toURI());
            if (containers.length > 0)
            {
                return Optional.of(containers[0].getProject()).filter(IProject::isAccessible);
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

            return Optional.ofNullable(bestMatch).filter(IProject::isAccessible);
        }

        return Optional.empty();
    }
}
