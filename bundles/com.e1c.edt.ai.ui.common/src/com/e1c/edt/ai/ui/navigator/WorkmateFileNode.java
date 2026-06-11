/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

import java.util.Objects;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.skills.SkillSource;

/**
 * A {@code WORKMATE.md} rule file at a given level ({@link SkillSource#PROJECT},
 * {@link SkillSource#WORKSPACE} or {@link SkillSource#USER}) shown under the {@link WorkmateGroupNode}.
 */
public class WorkmateFileNode
{
    private final IProject project;
    private final SkillSource level;

    public WorkmateFileNode(IProject project, SkillSource level)
    {
        this.project = project;
        this.level = level;
    }

    public IProject getProject()
    {
        return project;
    }

    public SkillSource getLevel()
    {
        return level;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof WorkmateFileNode))
        {
            return false;
        }
        var other = (WorkmateFileNode)obj;
        return level == other.level && Objects.equals(project, other.project);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(project, level);
    }
}
