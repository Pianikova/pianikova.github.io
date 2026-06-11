/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves a single skill resource (a {@code SKILL.md} or a {@code tools/<tool>.json}) by
 * checking the {@code .workmate} override levels in priority order
 * {@code project > workspace > user} and falling back to the bundled default.
 * <p>
 * Resolution is per file: a user may override just the {@code SKILL.md} or just one tool schema;
 * each resource is resolved independently.
 */
public interface ISkillResourceResolver
{
    /**
     * Resolves a skill resource.
     *
     * @param relativePath the bundle-relative resource path, e.g. {@code skills/git-commit/SKILL.md};
     *            override files are looked up under {@code <level>/.workmate/<relativePath>}.
     * @param projectRoot the project root directory providing the project-level override, or empty
     *            to skip that level.
     * @return the resolved resource, or empty if it is not found at any level.
     */
    Optional<ResolvedSkillResource> resolve(String relativePath, Optional<Path> projectRoot);

    /**
     * Tells whether an override file for the resource exists at the given level (used for the
     * "overridden" badge in the navigator).
     *
     * @param level the level to check ({@code BUNDLE} always returns {@code false}).
     * @param relativePath the bundle-relative resource path, e.g. {@code skills/git-commit/SKILL.md}.
     * @param projectRoot the project root for {@link SkillSource#PROJECT}, or empty.
     * @return {@code true} if the file physically exists at that level.
     */
    boolean existsAt(SkillSource level, String relativePath, Optional<Path> projectRoot);
}
