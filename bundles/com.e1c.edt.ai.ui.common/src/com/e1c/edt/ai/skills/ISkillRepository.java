/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

/**
 * @author Bogdan Sushkov
 *
 */
public interface ISkillRepository
{
    String loadSkillMarkdown(String skillId);

    String loadToolRequestSchema(String skillId, String toolId);
}
