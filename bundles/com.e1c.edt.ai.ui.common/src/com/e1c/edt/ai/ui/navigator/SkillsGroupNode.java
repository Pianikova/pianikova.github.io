/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

import java.util.Objects;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.skills.SkillSource;

/**
 * "Skills" group node shown under a {@link ScopeNode}. Its children are the skills, each editable at
 * this scope's level.
 */
public class SkillsGroupNode
{
    private final SkillSource level;
    private final IProject project;

    public SkillsGroupNode(SkillSource level, IProject project)
    {
        this.level = level;
        this.project = project;
    }

    public SkillSource getLevel()
    {
        return level;
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
        if (!(obj instanceof SkillsGroupNode))
        {
            return false;
        }
        var other = (SkillsGroupNode)obj;
        return level == other.level && Objects.equals(project, other.project);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(level, project);
    }
}
