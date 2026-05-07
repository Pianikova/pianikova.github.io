/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.assistent.SkillErrorCode;
import com.e1c.edt.ai.assistent.SkillExecutionException;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class ToolRequestSpecificationParser
{
    private final IJson json;

    @Inject
    public ToolRequestSpecificationParser(IJson json)
    {
        Preconditions.checkNotNull(json);
        this.json = json;
    }

    public ToolRequestSpecification parse(String toolId, String jsonString)
    {
        try {
            var specOpt = json.deserialize(jsonString, ToolRequestSpecification.class);
            var spec = specOpt.orElseThrow(() -> new SkillExecutionException(SkillErrorCode.TOOL_REQUEST_PARSE_ERROR,
                "Tool request specification is null: " + toolId)); //$NON-NLS-1$

            spec.validate(toolId);
            return spec;
        }
        catch (SkillExecutionException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new SkillExecutionException(SkillErrorCode.TOOL_REQUEST_PARSE_ERROR,
                "Invalid JSON for tool request: " + toolId, e); //$NON-NLS-1$
        }
    }
}
