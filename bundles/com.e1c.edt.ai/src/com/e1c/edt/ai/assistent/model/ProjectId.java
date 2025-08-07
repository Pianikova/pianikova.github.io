/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import org.eclipse.core.resources.IProject;

import com.google.common.base.Preconditions;

public class ProjectId
{
    public final String path;
    public final IProject project;

    public ProjectId(String path, IProject project)
    {
        Preconditions.checkNotNull(path);
        this.path = path;
        this.project = project;
    }

    @Override
    public String toString()
    {
        return path;
    }
}
