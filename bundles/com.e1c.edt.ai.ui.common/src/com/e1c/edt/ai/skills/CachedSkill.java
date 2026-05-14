/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.e1c.edt.ai.assistent.SkillErrorCode;
import com.e1c.edt.ai.assistent.SkillExecutionException;
import com.e1c.edt.ai.assistent.model.SkillMetadata;

/**
 * @author Bogdan Sushkov
 *
 */
public class CachedSkill
{
    private final String skillId;
    private final SkillMetadata metadata;
    private final String template;
    private final Set<String> toolIds;
    private final Map<String, String> toolRequestTemplates;

    public CachedSkill(String skillId, SkillMetadata metadata, String template, Set<String> toolIds,
        Map<String, String> toolRequestTemplates)
    {
        this.skillId = skillId;
        this.metadata = metadata;
        this.template = template;
        this.toolIds = Collections.unmodifiableSet(new LinkedHashSet<>(toolIds));
        this.toolRequestTemplates = toolRequestTemplates == null ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(toolRequestTemplates));
    }

    public CachedSkill copyWithToolRequestTemplates(Map<String, String> toolRequestTemplates)
    {
        return new CachedSkill(skillId, metadata, template, toolIds, toolRequestTemplates);
    }

    public String getSkillId()
    {
        return skillId;
    }

    public SkillMetadata getMetadata()
    {
        return metadata;
    }

    public String getTemplate()
    {
        return template;
    }

    public Set<String> getToolIds()
    {
        return toolIds;
    }

    @SuppressWarnings("nls")
    public String getToolRequestTemplate(String toolId)
    {
        var template = toolRequestTemplates.get(toolId);
        if (template == null)
        {
            throw new SkillExecutionException(SkillErrorCode.TOOL_REQEST_NOT_FOUND,
                "Tool request not found in cached skill: " + skillId + "/tools/" + toolId + ".json");
        }
        return template;
    }

}
