/**
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Test;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.RegexTemplateProcessor;
import com.e1c.edt.ai.assistent.SkillExecutionException;

/**
 * Contract tests for the bundled text-* skills used by the context-menu actions: every SKILL.md
 * must reference the visual_context tool and use exactly the parameters supplied by
 * ContextMenuInterceptor.
 */
@SuppressWarnings("nls")
public class TextSkillsContractTest
{
    private static final String[] SKILL_IDS =
        { "text-suggest", "text-correct-errors", "text-in-other-words", "text-improve-style" };

    /** The parameter contract of ContextMenuInterceptor.executeAction. */
    private static final Map<String, String> PARAMETERS = Map.of(
        "field_name", "Имя",
        "field_value", "ОбработкаВозврата",
        "selected_text", "",
        "is_multiline", "false",
        "language", "Русский");

    // Tycho surefire runs with the test project as the working directory
    private static final Path SKILLS_DIR = Path.of("..", "..", "bundles", "com.e1c.edt.ai.ui.common", "skills");

    private final RegexTemplateProcessor regexTemplateProcessor = new RegexTemplateProcessor();
    private final SkillTemplateProcessor templateProcessor =
        new SkillTemplateProcessor(regexTemplateProcessor, mock(IJson.class));
    private final SkillMdParser parser = new SkillMdParser(templateProcessor);

    @Test
    public void skillsDeclareVisualContextToolAndMetadata() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILLS_DIR));

        for (var skillId : SKILL_IDS)
        {
            var skill = parser.parse(skillId, readSkillMd(skillId));

            assertEquals(skillId, skill.getMetadata().getValues().get("name"));
            assertFalse(skill.getMetadata().getValues().get("description").isBlank());
            assertTrue(skillId + " must explicitly disable model tools",
                skill.getMetadata().getAllowedTools().isPresent());
            assertTrue(skillId + " must not expose model tools",
                skill.getMetadata().getAllowedTools().get().isEmpty());
            assertTrue(skillId + " must reference !tool('visual_context')",
                skill.getToolIds().contains("visual_context"));

            var toolJson = SKILLS_DIR.resolve(skillId).resolve("tools").resolve("visual_context.json");
            assertTrue(skillId + " must ship tools/visual_context.json", Files.isRegularFile(toolJson));
            assertTrue(Files.readString(toolJson, StandardCharsets.UTF_8).contains("\"GetVisualContext\""));
        }
    }

    @Test
    public void placeholdersResolveWithInterceptorParameters() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILLS_DIR));

        for (var skillId : SKILL_IDS)
        {
            var skill = parser.parse(skillId, readSkillMd(skillId));

            var resolved = templateProcessor.resolvePlaceholders(skill.getTemplate(), PARAMETERS);

            assertFalse(skillId + " must not leave unresolved placeholders", resolved.contains("${"));
            assertTrue(resolved.contains("ОбработкаВозврата"));
        }
    }

    @Test
    public void missingParameterFailsExecution() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILLS_DIR));

        var skill = parser.parse(SKILL_IDS[0], readSkillMd(SKILL_IDS[0]));
        var incomplete = Map.of("field_name", "Имя");

        try
        {
            templateProcessor.resolvePlaceholders(skill.getTemplate(), incomplete);
            fail("Should throw for a missing skill parameter");
        }
        catch (SkillExecutionException e)
        {
            assertEquals(com.e1c.edt.ai.assistent.SkillErrorCode.MISSING_PARAMETER, e.getCode());
        }
    }

    private String readSkillMd(String skillId) throws IOException
    {
        return Files.readString(SKILLS_DIR.resolve(skillId).resolve("SKILL.md"), StandardCharsets.UTF_8);
    }
}
