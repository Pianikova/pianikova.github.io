/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.RegexTemplateProcessor;

/**
 * Contract tests for the bundled background-code-analysis skill.
 */
@SuppressWarnings("nls")
public class BackgroundCodeAnalysisSkillContractTest
{
    private static final Path SKILL_PATH = Path.of("..", "..", "bundles", "com.e1c.edt.ai.ui.common",
        "skills", "background-code-analysis", "SKILL.md");

    @Test
    public void skillExposesOnlyToolsRequiredByItsWorkflow() throws IOException
    {
        assumeTrue(Files.isRegularFile(SKILL_PATH));

        var processor = new SkillTemplateProcessor(new RegexTemplateProcessor(), Mockito.mock(IJson.class));
        var skill = new SkillMdParser(processor)
            .parse("background-code-analysis", Files.readString(SKILL_PATH, StandardCharsets.UTF_8));

        assertTrue(skill.getMetadata().getAllowedTools().isPresent());
        assertEquals(Set.of("LocalHistory", "LocalChanges", "Read", "SetMarkers"),
            skill.getMetadata().getAllowedTools().get());
        assertTrue(skill.getMetadata().getCompletionPolicy().isPresent());
        assertEquals("#END#", skill.getMetadata().getCompletionPolicy().get().getMarker());
        assertTrue(skill.getMetadata().getCompletionPolicy().get().isRejectToolLikeJson());
    }
}
