/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

import java.util.Objects;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.skills.SkillSource;

/**
 * A configuration scope node ({@link SkillSource#USER}, {@link SkillSource#WORKSPACE} or
 * {@link SkillSource#PROJECT}). Its children are the scope's {@code WORKMATE.md} and the skills group.
 * For {@link SkillSource#PROJECT} the project is set; for the other levels it is {@code null}.
 */
public class ScopeNode
{
    private final SkillSource level;
    private final IProject project;

    public ScopeNode(SkillSource level, IProject project)
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
        if (!(obj instanceof ScopeNode))
        {
            return false;
        }
        var other = (ScopeNode)obj;
        return level == other.level && Objects.equals(project, other.project);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(level, project);
    }
}
