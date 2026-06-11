/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;

/**
 * Provides the base directories of the {@code .workmate} configuration levels
 * (user home / workspace / project). Shared by the open-file handlers and by the
 * skill resource resolver so that the {@code .workmate} path-resolution logic lives
 * in a single place.
 */
public interface IWorkmateLocations
{
    /** Name of the configuration directory at each level. */
    String WORKMATE_DIR = ".workmate"; //$NON-NLS-1$

    /**
     * @return the user home directory, or empty if it cannot be determined.
     */
    Optional<Path> userHome();

    /**
     * @return the workspace root directory, or empty if it cannot be determined.
     */
    Optional<Path> workspaceRoot();

    /**
     * @param project the project, may be {@code null}.
     * @return the project location directory, or empty if it cannot be determined.
     */
    Optional<Path> projectRoot(IProject project);

    /**
     * Resolves the given relative path segments against a base directory.
     *
     * @param base the base directory, not {@code null}.
     * @param segments the relative path segments, not {@code null}.
     * @return the resolved absolute path.
     */
    static Path resolve(Path base, List<String> segments)
    {
        var path = base;
        for (var segment : segments)
        {
            path = path.resolve(segment);
        }
        return path;
    }
}
