/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import java.util.Map;
import java.util.Optional;

import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillRegistry
    implements ISkillRegistry
{
    private final Map<String, ISkill> skills;

    @Inject
    public SkillRegistry(Map<String, ISkill> skills)
    {
        this.skills = skills;
    }

    @Override
    public Optional<ISkill> findById(String id)
    {
        return Optional.ofNullable(skills.get(id));
    }

}
