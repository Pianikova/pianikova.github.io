/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillCache
    implements ISkillCache
{
    private final ConcurrentMap<String, CachedSkill> cache = new ConcurrentHashMap<>();

    @Inject
    public SkillCache()
    {
    }

    @Override
    public Optional<CachedSkill> get(String skillId)
    {
        return Optional.ofNullable(cache.get(skillId));
    }

    @Override
    public CachedSkill computeIfAbsent(String skillId, Supplier<? extends CachedSkill> supplier)
    {
        return cache.computeIfAbsent(skillId, k -> supplier.get());
    }
}
