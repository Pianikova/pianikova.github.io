/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillPackageLoader
{
    private final ISkillRepository repository;
    private final ISkillMdParser skillMdParser;

    @Inject
    public SkillPackageLoader(ISkillRepository repository, ISkillMdParser parser)
    {
        Preconditions.checkNotNull(repository);
        Preconditions.checkNotNull(parser);
        this.repository = repository;
        this.skillMdParser = parser;
    }

    public CachedSkill load(String skillId, Optional<Path> projectRoot)
    {
        var skillMd = repository.loadSkillMarkdown(skillId, projectRoot);
        CachedSkill parsedSkill = skillMdParser.parse(skillId, skillMd);

        var toolRequestTemplates = new LinkedHashMap<String, String>();

        for (var toolId : parsedSkill.getToolIds())
        {
            var toolRequestTemplate = repository.loadToolRequestSchema(skillId, toolId, projectRoot);
            toolRequestTemplates.put(toolId, toolRequestTemplate);
        }

        return parsedSkill.copyWithToolRequestTemplates(toolRequestTemplates);
    }
}
