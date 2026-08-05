/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

/**
 * Contract tests for the bundled git-commit skill.
 */
@SuppressWarnings("nls")
public class GitCommitSkillContractTest
{
    // Tycho surefire runs with the test project as the working directory.
    private static final Path SKILL_DIR =
        Path.of("..", "..", "bundles", "com.e1c.edt.ai.ui.common", "skills", "git-commit");

    @Test
    public void skillRequiresStructuredJGitCallsWithWorkingDirectory() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILL_DIR));

        var skill = read("SKILL.md");

        assertTrue(skill.contains("allowed-tools: JGit"));
        assertFalse(skill.contains("allowed-tools: JGit Read"));
        assertTrue(skill.contains("completion-marker: \"#END#\""));
        assertTrue(skill.contains("reject-tool-like-json: true"));
        assertTrue(skill.contains("вызывай только `JGit`"));
        assertTrue(skill.contains("\"working_directory\": \"${working_directory}\""));
        assertTrue(skill.contains("[\"diff\", \"--cached\", \"--\", <не более трёх путей>]"));
        assertTrue(skill.contains("Пакеты из четырёх и более файлов запрещены"));
        assertTrue(skill.contains("вызывай их строго по одному"));
        assertTrue(skill.contains("дождись результата и только затем вызывай `JGit`"));
        assertTrue(skill.contains("Не отправляй несколько function calls в одном ответе"));
        assertTrue(skill.contains("ровно один настоящий function call"));
        assertTrue(skill.contains("Не вызывай `TodoWrite`"));
        assertTrue(skill.contains("Вторая строка должна быть строго пустой"));
        assertTrue(skill.contains("Не добавляй после деталей"));
        assertFalse(skill.contains("[TOOL_CALL:"));
        assertFalse(skill.contains("```json"));
    }

    /**
     * The answer is written into the commit message field verbatim, so the skill must forbid any
     * preamble, reasoning or code-fence wrapping around the message.
     */
    @Test
    public void skillForbidsPreambleAndFencingAroundCommitMessage() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILL_DIR));

        var skill = read("SKILL.md");

        assertTrue(skill.contains("Ответ обязан начинаться первым символом строки-резюме"));
        assertTrue(skill.contains("Не оборачивай commit-message в блок кода"));
        assertTrue(skill.contains("Проверки длины и полноты выполняй молча"));
        assertTrue(skill.contains("Неправильный финальный ответ"));
    }

    @Test
    public void predefinedGitRequestsUseSupportedJGitArguments() throws IOException
    {
        assumeTrue(Files.isDirectory(SKILL_DIR));

        var diffList = read("tools/git_diff_list.json");
        var recentCommits = read("tools/git_recent_commits.json");

        assertTrue(diffList.contains("\"name\": \"JGit\""));
        assertTrue(diffList.contains("\"args\": [\"diff\", \"--cached\", \"--name-status\"]"));
        assertTrue(recentCommits.contains("\"name\": \"JGit\""));
        assertTrue(recentCommits.contains("\"--oneline\""));
        assertFalse(recentCommits.contains("--pretty"));
    }

    private static String read(String relativePath) throws IOException
    {
        return Files.readString(SKILL_DIR.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
