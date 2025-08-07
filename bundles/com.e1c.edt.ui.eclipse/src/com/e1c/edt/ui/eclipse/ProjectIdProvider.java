/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import java.util.Optional;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.assistent.model.ProjectId;

class ProjectIdProvider
    implements IProjectIdProvider
{
    @Override
    public ProjectId getProjectId(IProject project)
    {
        return new ProjectId(project.getLocationURI().getPath(), project);
    }

    @Override
    public Optional<ProjectId> getProjectId(String filePath, ICancellationToken cancellationToken)
    {
        return Optional.of(new ProjectId(filePath, null));
    }
}