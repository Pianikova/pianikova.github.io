/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.RegexTemplateProcessor;

/**
 * Interactive chat skills must not use the autonomous completion marker: chat does not strip it.
 */
@SuppressWarnings("nls")
public class InteractiveSkillsCompletionContractTest
{
    private static final String[] INTERACTIVE_SKILLS =
        { "git-review", "quick-fix-ai-marker", "quick-fix-problem", "quick-fix-configuration-problem" };

    private static final Path SKILLS_DIR =
        Path.of("..", "..", "bundles", "com.e1c.edt.ai.ui.common", "skills");

    @Test
    public void interactiveSkillsDoNotDeclareCompletionPolicy() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILLS_DIR));
        var processor = new SkillTemplateProcessor(new RegexTemplateProcessor(), mock(IJson.class));
        var parser = new SkillMdParser(processor);

        for (var skillId : INTERACTIVE_SKILLS)
        {
            var markdown = Files.readString(SKILLS_DIR.resolve(skillId).resolve("SKILL.md"),
                StandardCharsets.UTF_8);
            assertFalse(skillId + " must leave final-answer handling to interactive chat",
                parser.parse(skillId, markdown).getMetadata().getCompletionPolicy().isPresent());
        }
    }
}
