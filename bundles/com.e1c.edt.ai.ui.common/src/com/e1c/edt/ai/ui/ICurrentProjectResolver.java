/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.core.resources.IProject;

public interface ICurrentProjectResolver
{
    Optional<IProject> resolve();

    Optional<IProject> resolve(String path);
}
