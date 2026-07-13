/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import com.e1c.edt.ai.assistent.SkillErrorCode;
import com.e1c.edt.ai.assistent.SkillExecutionException;
import com.google.gson.JsonObject;

/**
 * @author Bogdan Sushkov
 *
 */
public class ToolRequestSpecification
{
    private String name;
    private JsonObject arguments;

    public ToolRequestSpecification(String name, JsonObject arguments)
    {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName()
    {
        return name;
    }

    public JsonObject getArguments()
    {
        return arguments;
    }

    @SuppressWarnings("nls")
    public void validate(String toolId)
    {
        if (name == null || name.trim().isEmpty())
        {
            throw new SkillExecutionException(SkillErrorCode.TOOL_REQUEST_PARSE_ERROR,
                "Tool request '" + toolId + "' must contain non-empty 'name' property");
        }
        if (arguments == null)
        {
            throw new SkillExecutionException(SkillErrorCode.TOOL_REQUEST_PARSE_ERROR,
                "Tool request '" + toolId + "' must contain 'arguments' property");
        }
    }
}
