/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

/**
 * @author Bogdan Sushkov
 *
 */
public interface ISkillMdParser
{
    CachedSkill parse(String skillId, String skillContent);
}
