/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A skill discovered by the {@link ISkillRegistry}: its identifier, display metadata, the tools it
 * declares, and the level its effective {@code SKILL.md} was resolved from.
 */
public class SkillDescriptor
{
    private final String skillId;
    private final String name;
    private final String description;
    private final SkillSource source;
    private final List<String> toolIds;

    /**
     * @param skillId the skill identifier (its directory name), not {@code null}.
     * @param name the display name from the frontmatter, falling back to the id, not {@code null}.
     * @param description the description from the frontmatter, may be {@code null}.
     * @param source the level the effective {@code SKILL.md} was resolved from, not {@code null}.
     * @param toolIds the tool identifiers declared by the skill, not {@code null}.
     */
    public SkillDescriptor(String skillId, String name, String description, SkillSource source, List<String> toolIds)
    {
        this.skillId = skillId;
        this.name = name;
        this.description = description;
        this.source = source;
        this.toolIds = toolIds == null ? List.of() : List.copyOf(toolIds);
    }

    public String getSkillId()
    {
        return skillId;
    }

    public String getName()
    {
        return name;
    }

    public Optional<String> getDescription()
    {
        return Optional.ofNullable(description);
    }

    public SkillSource getSource()
    {
        return source;
    }

    /**
     * @return the tool identifiers declared by the skill (from {@code !tool('...')} directives),
     *     never {@code null}.
     */
    public List<String> getToolIds()
    {
        return Collections.unmodifiableList(toolIds);
    }
}
