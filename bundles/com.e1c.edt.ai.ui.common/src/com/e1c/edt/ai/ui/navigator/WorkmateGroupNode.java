/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

import java.util.Objects;

import org.eclipse.core.resources.IProject;

/**
 * Root "Workmate" group node shown under a project in the navigator. Its children are the
 * {@code WORKMATE.md} rule files (per level) and the {@link SkillsGroupNode}.
 */
public class WorkmateGroupNode
{
    private final IProject project;

    public WorkmateGroupNode(IProject project)
    {
        this.project = project;
    }

    public IProject getProject()
    {
        return project;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof WorkmateGroupNode))
        {
            return false;
        }
        return Objects.equals(project, ((WorkmateGroupNode)obj).project);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(project);
    }
}
