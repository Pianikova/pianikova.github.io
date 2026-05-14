/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillsModule
    extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(ISkillRepository.class).to(SkillRepository.class).in(Singleton.class);
        bind(ISkillMdParser.class).to(SkillMdParser.class).in(Singleton.class);
        bind(ISkillCache.class).to(SkillCache.class).in(Singleton.class);
        bind(SkillTemplateProcessor.class).in(Singleton.class);
        bind(IToolDirectiveExecutor.class).to(ToolDirectiveExecutor.class).in(Singleton.class);
        bind(ToolRequestSpecificationParser.class).in(Singleton.class);
        bind(ISkillExecutor.class).to(SkillExecutor.class).in(Singleton.class);
        bind(IMcpToolInvoker.class).to(McpToolInvoker.class).in(Singleton.class);
        bind(SkillTemplateRenderer.class).in(Singleton.class);
    }
}
