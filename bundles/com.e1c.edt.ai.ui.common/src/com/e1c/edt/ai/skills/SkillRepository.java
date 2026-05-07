/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import com.e1c.edt.ai.assistent.SkillErrorCode;
import com.e1c.edt.ai.assistent.SkillExecutionException;
import com.e1c.edt.ai.ui.IResourceProvider;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillRepository
    implements ISkillRepository
{
    private static final String SKILLS_DIR = "skills"; //$NON-NLS-1$
    private static final String SKILL_MD = "SKILL.md"; //$NON-NLS-1$
    private static final String TOOLS_DIR = "tools"; //$NON-NLS-1$

    private final IResourceProvider resourceProvider;

    @Inject
    public SkillRepository(IResourceProvider resourceProvider)
    {
        Preconditions.checkNotNull(resourceProvider);
        this.resourceProvider = resourceProvider;
    }

    @Override
    public String loadSkillMarkdown(String skillId)
    {
        String path = SKILLS_DIR + "/" + skillId + "/" + SKILL_MD; //$NON-NLS-1$ //$NON-NLS-2$
        return resourceProvider.getTextResource(path)
            .orElseThrow(
                () -> new SkillExecutionException(SkillErrorCode.SKILL_NOT_FOUND, "Resource not found: " + path)); //$NON-NLS-1$
    }

    @Override
    public String loadToolRequestSchema(String skillId, String toolId)
    {
        String resourcePath = SKILLS_DIR + "/" + skillId + "/" + TOOLS_DIR + "/" + toolId + ".json"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        return resourceProvider.getTextResource(resourcePath)
            .orElseThrow(() -> new SkillExecutionException(SkillErrorCode.TOOL_REQEST_NOT_FOUND,
                "Resource not found: " + resourcePath)); //$NON-NLS-1$
    }
}
