/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.mockito.Mockito;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.RegexTemplateProcessor;

/**
 * Contract tests for the bundled git-review skill.
 * <p>
 * The skill carries instructions only: no data is substituted into the prompt and no tool is invoked
 * on its behalf. The model fetches the file list and the diffs itself, in batches, with the exact
 * parameters the skill spells out. Injecting data here is what made the skill unusable — the staged
 * diff of a real configuration is over ten megabytes.
 */
@SuppressWarnings("nls")
public class GitReviewSkillContractTest
{
    // Tycho surefire runs with the test project as the working directory.
    private static final Path SKILL_DIR =
        Path.of("..", "..", "bundles", "com.e1c.edt.ai.ui.common", "skills", "git-review");

    @Test
    public void skillCarriesInstructionsAndNoData() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILL_DIR));

        var skill = read("SKILL.md");

        assertFalse("the skill must not invoke tools on its own — no data belongs in the prompt",
            skill.contains("!tool("));
        assertFalse("with no tool directives left, the tools folder must be gone",
            Files.exists(SKILL_DIR.resolve("tools")));
        assertTrue(skill.contains("В этом скилле нет данных"));
        assertTrue(skill.contains("Как получить данные"));
    }

    @Test
    public void skillSpellsOutHowToFetchTheFileListItself() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILL_DIR));

        var skill = read("SKILL.md");

        assertTrue(skill.contains("[\"diff\", \"--cached\", \"--name-status\"]"));
        assertTrue(skill.contains("`Read`"));
    }

    @Test
    public void skillRequiresBatchedJGitCallsWithWorkingDirectory() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILL_DIR));

        var skill = read("SKILL.md");

        assertTrue(skill.contains("вызывай только `JGit`"));
        assertTrue(skill.contains("\"working_directory\": \"${working_directory}\""));
        assertTrue(skill.contains("[\"diff\", \"--cached\", \"--\", <не более трёх путей>]"));
        assertTrue(skill.contains("Пакеты из четырёх и более файлов запрещены"));
        assertTrue(skill.contains("отправь все function calls одновременно"));
        assertTrue(skill.contains("Повторять уже полученные пакеты запрещено"));
        assertTrue(skill.contains("Не вызывай"));
    }

    @Test
    public void skillBoundsTheScopeAndReportsWhatItSkipped() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILL_DIR));

        var skill = read("SKILL.md");

        assertTrue(skill.contains("Отбор файлов для ревью"));
        assertTrue(skill.contains("Не запрашивай дифф для сгенерированных выгрузок"));
        assertTrue(skill.contains("Молча урезать охват нельзя"));
        assertTrue(skill.contains("Не проверено"));
    }

    @Test
    public void skillKeepsToolsUnrestrictedForTheFixStep() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILL_DIR));

        var processor = new SkillTemplateProcessor(new RegexTemplateProcessor(), Mockito.mock(IJson.class));
        var skill = new SkillMdParser(processor).parse("git-review", read("SKILL.md"));

        assertEquals("git-review", skill.getMetadata().getValues().get("name"));
        // The review offers to apply the fixes afterwards, which needs Read/Edit and the marker
        // tools — an allowed-tools list here would silently break that step.
        assertFalse("git-review must stay unrestricted so the fix step keeps working",
            skill.getMetadata().getAllowedTools().isPresent());
    }

    private static String read(String relativePath) throws IOException
    {
        return Files.readString(SKILL_DIR.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
