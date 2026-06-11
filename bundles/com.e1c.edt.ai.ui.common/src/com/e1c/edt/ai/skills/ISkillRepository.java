/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Bogdan Sushkov
 *
 */
public interface ISkillRepository
{
    /**
     * Loads the effective {@code SKILL.md} for a skill, honouring {@code .workmate} overrides.
     *
     * @param skillId the skill identifier.
     * @param projectRoot the project root providing the project-level override, or empty to skip it.
     * @return the resolved markdown content.
     */
    String loadSkillMarkdown(String skillId, Optional<Path> projectRoot);

    /**
     * Loads the effective tool request schema for a skill, honouring {@code .workmate} overrides.
     *
     * @param skillId the skill identifier.
     * @param toolId the tool identifier.
     * @param projectRoot the project root providing the project-level override, or empty to skip it.
     * @return the resolved JSON schema content.
     */
    String loadToolRequestSchema(String skillId, String toolId, Optional<Path> projectRoot);
}
