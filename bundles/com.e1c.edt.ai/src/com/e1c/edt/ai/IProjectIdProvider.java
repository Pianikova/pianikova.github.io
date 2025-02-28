/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

import com.e1c.edt.ai.assistent.model.ProjectId;
import org.eclipse.core.resources.IProject;

public interface IProjectIdProvider
{
    ProjectId getProjectId(IProject project);

    Optional<ProjectId> getProjectId(String filePath, ICancellationToken cancellationToken);
}
