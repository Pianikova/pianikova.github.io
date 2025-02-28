/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import java.util.Optional;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.assistent.model.ProjectId;
import org.eclipse.core.resources.IProject;

class ProjectIdProvider
    implements IProjectIdProvider
{
    @Override
    public ProjectId getProjectId(IProject project)
    {
        return new ProjectId(project.getFullPath().toPortableString());
    }

    @Override
    public Optional<ProjectId> getProjectId(String filePath, ICancellationToken cancellationToken)
    {
        return Optional.of(new ProjectId(filePath));
    }

}
