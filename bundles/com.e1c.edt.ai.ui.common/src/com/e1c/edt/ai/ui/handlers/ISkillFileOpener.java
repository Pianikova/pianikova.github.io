/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import java.util.Optional;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.skills.SkillSource;

/**
 * Opens (creating if necessary) the AI configuration files of the {@code .workmate} hierarchy in an
 * editor. Skill files are seeded with the current effective content; {@code WORKMATE.md} files are
 * created empty.
 */
public interface ISkillFileOpener
{
    /**
     * Opens, creating if necessary, the {@code SKILL.md} of a skill at the given level.
     *
     * @param skillId the skill identifier, not {@code null}.
     * @param level the override level to open/create at; {@link SkillSource#BUNDLE} is not editable
     *            and is ignored.
     * @param project the project for the project level, or empty.
     */
    void openSkill(String skillId, SkillSource level, Optional<IProject> project);

    /**
     * Opens, creating if necessary, the {@code WORKMATE.md} at the given level.
     *
     * @param level the level to open/create at; {@link SkillSource#BUNDLE} is ignored.
     * @param project the project for the project level, or empty.
     */
    void openWorkmate(SkillSource level, Optional<IProject> project);

    /**
     * Opens, creating if necessary, a skill tool schema {@code tools/<toolId>.json} at the given level.
     *
     * @param skillId the skill identifier, not {@code null}.
     * @param toolId the tool identifier, not {@code null}.
     * @param level the override level to open/create at; {@link SkillSource#BUNDLE} is ignored.
     * @param project the project for the project level, or empty.
     */
    void openSkillTool(String skillId, String toolId, SkillSource level, Optional<IProject> project);

    /**
     * Removes the {@code SKILL.md} override at the given level (reverting to the inherited/bundled one).
     *
     * @param skillId the skill identifier, not {@code null}.
     * @param level the level whose override file is deleted.
     * @param project the project for the project level, or empty.
     */
    void resetSkill(String skillId, SkillSource level, Optional<IProject> project);

    /**
     * Removes the tool schema override {@code tools/<toolId>.json} at the given level.
     *
     * @param skillId the skill identifier, not {@code null}.
     * @param toolId the tool identifier, not {@code null}.
     * @param level the level whose override file is deleted.
     * @param project the project for the project level, or empty.
     */
    void resetSkillTool(String skillId, String toolId, SkillSource level, Optional<IProject> project);

    /**
     * Removes the {@code WORKMATE.md} file at the given level.
     *
     * @param level the level whose file is deleted.
     * @param project the project for the project level, or empty.
     */
    void resetWorkmate(SkillSource level, Optional<IProject> project);
}
