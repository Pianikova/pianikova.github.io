/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;

/**
 * Discovers the available skills as the union of the bundled defaults and the user overrides found
 * in the {@code .workmate/skills} directories of the user, workspace and (optionally) project levels.
 */
public interface ISkillRegistry
{
    /**
     * Lists the available skills.
     *
     * @param project the project whose project-level overrides should be included, or empty to
     *            consider only the user and workspace levels plus the bundled defaults.
     * @return the discovered skills, one descriptor per skill id.
     */
    List<SkillDescriptor> listSkills(Optional<IProject> project);
}
