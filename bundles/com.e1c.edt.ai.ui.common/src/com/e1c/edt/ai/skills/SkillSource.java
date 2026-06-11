/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

/**
 * The {@code .workmate} level a skill resource was resolved from. Levels are listed in
 * descending priority order; {@link #BUNDLE} is the read-only default shipped with the plugin.
 */
public enum SkillSource
{
    PROJECT,
    WORKSPACE,
    USER,
    BUNDLE;

    /**
     * @return {@code true} if the resource comes from a user-editable {@code .workmate} level
     *     (i.e. it overrides the bundled default).
     */
    public boolean isOverride()
    {
        return this != BUNDLE;
    }
}
