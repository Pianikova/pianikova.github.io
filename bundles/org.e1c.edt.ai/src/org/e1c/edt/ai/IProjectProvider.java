/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import org.eclipse.core.resources.IProject;

public interface IProjectProvider
{
    Optional<IProject> getProject(String filePath);
}
