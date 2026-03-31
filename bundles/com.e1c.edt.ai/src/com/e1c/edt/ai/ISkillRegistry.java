/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import java.util.Optional;

/**
 * @author Bogdan Sushkov
 *
 */
public interface ISkillRegistry
{
    Optional<ISkill> findById(String id);
}
