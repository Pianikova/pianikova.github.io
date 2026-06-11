/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.nio.file.Path;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import com.google.inject.Singleton;

/**
 * Default {@link IWorkmateLocations} implementation backed by the running platform.
 */
@Singleton
public class WorkmateLocations
    implements IWorkmateLocations
{
    @Override
    public Optional<Path> userHome()
    {
        return Optional.ofNullable(System.getProperty("user.home")).map(Path::of); //$NON-NLS-1$
    }

    @Override
    public Optional<Path> workspaceRoot()
    {
        return Optional.ofNullable(ResourcesPlugin.getWorkspace().getRoot().getLocation())
            .map(location -> Path.of(location.toOSString()));
    }

    @Override
    public Optional<Path> projectRoot(IProject project)
    {
        if (project == null)
        {
            return Optional.empty();
        }
        return Optional.ofNullable(project.getLocation()).map(location -> Path.of(location.toOSString()));
    }
}
