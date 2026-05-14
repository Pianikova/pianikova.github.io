/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillExecutionRequest
{
    private final String skillId;
    private final Map<String, String> parameters;

    public SkillExecutionRequest(String skillId, Map<String, String> parameters)
    {
        Preconditions.checkNotNull(skillId);
        this.skillId = skillId;
        this.parameters = parameters;
    }

    public String getSkillId()
    {
        return skillId;
    }

    public Map<String, String> getParameters()
    {
        return parameters;
    }
}
