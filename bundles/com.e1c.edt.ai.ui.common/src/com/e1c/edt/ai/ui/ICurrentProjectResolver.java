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

    /**
     * Resolves a project guaranteeing a result whenever the workspace has at least one accessible project.
     * Falls back to the first accessible project (stable order) when the current context yields none, so a
     * session/chat opened without an explicit project still gets a project (and its uuid). Empty only when the
     * workspace has no accessible projects at all.
     *
     * @return the resolved project, or empty only if there are no accessible projects
     */
    Optional<IProject> resolveOrDefault();
}
