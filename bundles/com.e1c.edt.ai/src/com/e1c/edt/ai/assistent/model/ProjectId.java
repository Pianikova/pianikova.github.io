/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Objects;

import org.eclipse.core.resources.IProject;

import com.google.common.base.Preconditions;

public class ProjectId
{
    public static final ProjectId Default = new ProjectId("", null); //$NON-NLS-1$
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

    @Override
    public int hashCode()
    {
        return Objects.hash(path, project);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProjectId other = (ProjectId)obj;
        return Objects.equals(path, other.path) && Objects.equals(project, other.project);
    }
}
