/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The result of resolving a single skill resource (a {@code SKILL.md} or a tool schema):
 * its content plus the level it was resolved from.
 */
public class ResolvedSkillResource
{
    private final String content;
    private final SkillSource source;
    private final Path path;

    /**
     * @param content the resource text, not {@code null}.
     * @param source the level it was resolved from, not {@code null}.
     * @param path the file system path for override levels, or {@code null} for the bundle.
     */
    public ResolvedSkillResource(String content, SkillSource source, Path path)
    {
        this.content = content;
        this.source = source;
        this.path = path;
    }

    public String getContent()
    {
        return content;
    }

    public SkillSource getSource()
    {
        return source;
    }

    /**
     * @return the file system path the resource was read from, or empty for the bundled default.
     */
    public Optional<Path> getPath()
    {
        return Optional.ofNullable(path);
    }
}
