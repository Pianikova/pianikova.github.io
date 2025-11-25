/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Objects;

import org.eclipse.core.resources.IProject;

public class ProjectId
{
    public static final ProjectId Default = new ProjectId(null);
    public final IProject project;

    public ProjectId(IProject project)
    {
        this.project = project;
    }

    @Override
    public String toString()
    {
        return project == null ? "[empty]" : project.getName(); //$NON-NLS-1$
    }

    @Override
    public int hashCode()
    {
        return project == null ? 0 : Objects.hash(project.getName());
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
        return Objects.equals(project.getName(), other.project.getName());
    }
}
