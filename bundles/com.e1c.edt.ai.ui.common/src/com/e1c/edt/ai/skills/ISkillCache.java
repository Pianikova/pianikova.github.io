/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Bogdan Sushkov
 *
 */
public interface ISkillCache
{
    Optional<CachedSkill> get(String skillId);

    CachedSkill computeIfAbsent(String skillId, Supplier<? extends CachedSkill> supplier);
}
