/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

import java.util.Objects;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.skills.SkillDescriptor;
import com.e1c.edt.ai.skills.SkillSource;

/**
 * A skill tool ({@code tools/<toolId>.json}) shown under a {@link SkillNode}, scoped to a level.
 */
public class SkillToolNode
{
    private final SkillSource level;
    private final IProject project;
    private final SkillDescriptor descriptor;
    private final String toolId;

    public SkillToolNode(SkillSource level, IProject project, SkillDescriptor descriptor, String toolId)
    {
        this.level = level;
        this.project = project;
        this.descriptor = descriptor;
        this.toolId = toolId;
    }

    public SkillSource getLevel()
    {
        return level;
    }

    public IProject getProject()
    {
        return project;
    }

    public SkillDescriptor getDescriptor()
    {
        return descriptor;
    }

    public String getToolId()
    {
        return toolId;
    }

    public String getSkillId()
    {
        return descriptor.getSkillId();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof SkillToolNode))
        {
            return false;
        }
        var other = (SkillToolNode)obj;
        return level == other.level && Objects.equals(project, other.project)
            && Objects.equals(descriptor.getSkillId(), other.descriptor.getSkillId())
            && Objects.equals(toolId, other.toolId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(level, project, descriptor.getSkillId(), toolId);
    }
}
