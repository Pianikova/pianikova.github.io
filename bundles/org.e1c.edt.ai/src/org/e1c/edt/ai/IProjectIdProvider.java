/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import org.e1c.edt.ai.assistent.model.ProjectId;

public interface IProjectIdProvider
{
    Optional<ProjectId> getProjectId(String filePath, ICancellationToken cancellationToken);
}
