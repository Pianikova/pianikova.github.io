/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.assistent.model.ProjectParameters;

/**
 * Поставщик параметров проекта для запроса создания сессии.
 */
public interface IProjectParametersProvider
{
    Optional<ProjectParameters> getProjectParameters(IProject project);
}
