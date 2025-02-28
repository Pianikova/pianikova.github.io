/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import java.util.Optional;

import com.e1c.edt.ai.IProjectProvider;
import org.eclipse.core.resources.IProject;

class ProjectProvider
    implements IProjectProvider
{
    @Override
    public Optional<IProject> getProject(String filePath)
    {
        return Optional.empty();
    }
}
