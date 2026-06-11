/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.nio.file.Path;
import java.util.Optional;

import com.e1c.edt.ai.assistent.SkillErrorCode;
import com.e1c.edt.ai.assistent.SkillExecutionException;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillRepository
    implements ISkillRepository
{
    static final String SKILLS_DIR = "skills"; //$NON-NLS-1$
    static final String SKILL_MD = "SKILL.md"; //$NON-NLS-1$
    static final String TOOLS_DIR = "tools"; //$NON-NLS-1$

    private final ISkillResourceResolver resolver;

    @Inject
    public SkillRepository(ISkillResourceResolver resolver)
    {
        Preconditions.checkNotNull(resolver);
        this.resolver = resolver;
    }

    @Override
    public String loadSkillMarkdown(String skillId, Optional<Path> projectRoot)
    {
        String path = skillMarkdownPath(skillId);
        return resolver.resolve(path, projectRoot)
            .map(ResolvedSkillResource::getContent)
            .orElseThrow(
                () -> new SkillExecutionException(SkillErrorCode.SKILL_NOT_FOUND, "Resource not found: " + path)); //$NON-NLS-1$
    }

    @Override
    public String loadToolRequestSchema(String skillId, String toolId, Optional<Path> projectRoot)
    {
        String path = toolSchemaPath(skillId, toolId);
        return resolver.resolve(path, projectRoot)
            .map(ResolvedSkillResource::getContent)
            .orElseThrow(() -> new SkillExecutionException(SkillErrorCode.TOOL_REQEST_NOT_FOUND,
                "Resource not found: " + path)); //$NON-NLS-1$
    }

    public static String skillMarkdownPath(String skillId)
    {
        return SKILLS_DIR + "/" + skillId + "/" + SKILL_MD; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static String toolSchemaPath(String skillId, String toolId)
    {
        return SKILLS_DIR + "/" + skillId + "/" + TOOLS_DIR + "/" + toolId + ".json"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
}
