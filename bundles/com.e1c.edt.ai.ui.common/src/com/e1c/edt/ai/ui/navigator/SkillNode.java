/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.navigator;

import java.util.Objects;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.skills.SkillDescriptor;
import com.e1c.edt.ai.skills.SkillSource;

/**
 * A single skill shown under a {@link SkillsGroupNode}, scoped to a level. Its children are the
 * skill's tools.
 */
public class SkillNode
{
    private final SkillSource level;
    private final IProject project;
    private final SkillDescriptor descriptor;

    public SkillNode(SkillSource level, IProject project, SkillDescriptor descriptor)
    {
        this.level = level;
        this.project = project;
        this.descriptor = descriptor;
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

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof SkillNode))
        {
            return false;
        }
        var other = (SkillNode)obj;
        return level == other.level && Objects.equals(project, other.project)
            && Objects.equals(descriptor.getSkillId(), other.descriptor.getSkillId());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(level, project, descriptor.getSkillId());
    }
}
