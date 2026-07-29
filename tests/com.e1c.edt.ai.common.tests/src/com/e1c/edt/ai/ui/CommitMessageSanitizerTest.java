/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for {@link CommitMessageSanitizer}. The two preamble cases below are verbatim answers the
 * git-commit skill produced in the Staging View after the SKILL.md rules forbidding them were added.
 */
@SuppressWarnings("nls")
public class CommitMessageSanitizerTest
{
    private static final String MESSAGE = "Добавление конфигураций Булочная, Платежные документы и Склад\n"
        + "\n"
        + "1) Новая функциональность в Булочная/src/CommonModules/ОбщегоНазначения/Module.bsl: серверные функции\n"
        + "    - добавлены функции для работы с датами, пользователями, справочниками\n"
        + "\n"
        + "2) Новая функциональность в Булочная/src/CommonModules/ОбщегоНазначенияКлиент/Module.bsl: клиент\n"
        + "    - добавлена процедура вывода сообщений пользователю";

    @Test
    public void keepsAWellFormedMessageUntouched()
    {
        assertEquals(MESSAGE, CommitMessageSanitizer.sanitize(MESSAGE));
    }

    @Test
    public void dropsAReasoningPreamble()
    {
        var answer = "Теперь у меня есть достаточно информации для формирования commit message. Все файлы\n"
            + "являются новыми (добавленными), и они относятся к трем конфигурациям.\n"
            + "\n"
            + "На основе предоставленного описания изменений и анализа файлов, сформирую commit message:\n"
            + "\n"
            + MESSAGE + "\n";

        assertEquals(MESSAGE, CommitMessageSanitizer.sanitize(answer));
    }

    @Test
    public void dropsAPreambleAndUnwrapsTheFencedMessage()
    {
        var answer = "Проанализировав diff, вижу:\n"
            + "\n"
            + "1. **Булочная/src/Configuration/Configuration.mdo** — новый файл конфигурации\n"
            + "2. **Склад/src/Configuration/CommandInterface.cmi** — новый файл интерфейса команд\n"
            + "\n"
            + "Оба файла добавлены (A). Первая строка: 69 символов (укладываемся в 72).\n"
            + "\n"
            + "```\n"
            + MESSAGE + "\n"
            + "```\n";

        assertEquals(MESSAGE, CommitMessageSanitizer.sanitize(answer));
    }

    @Test
    public void unwrapsAFencedMessageWithoutAPreamble()
    {
        assertEquals(MESSAGE, CommitMessageSanitizer.sanitize("```markdown\n" + MESSAGE + "\n```"));
    }

    @Test
    public void keepsDetailsThatHaveNoSummaryAbove()
    {
        var details = "1) Прочие изменения в .gitignore: игнорирование сборки\n    - добавлен target";

        assertEquals(details, CommitMessageSanitizer.sanitize(details));
    }

    @Test
    public void passesThroughAnAnswerWithoutDetailsSections()
    {
        var summaryOnly = "Правка опечатки в README";

        assertEquals(summaryOnly, CommitMessageSanitizer.sanitize(summaryOnly));
    }

    @Test
    public void stripsStrayFencesFromAnAnswerWithoutDetailsSections()
    {
        assertEquals("Правка опечатки в README", CommitMessageSanitizer.sanitize("```\nПравка опечатки в README\n"));
    }

    @Test
    public void keepsTheRawAnswerWhenCleanupWouldEmptyIt()
    {
        var fencesOnly = "```\n```";

        assertEquals(fencesOnly, CommitMessageSanitizer.sanitize(fencesOnly));
    }

    @Test
    public void normalizesWindowsLineEndings()
    {
        var answer = "Вступление:\r\n\r\n" + MESSAGE.replace("\n", "\r\n");

        assertEquals(MESSAGE, CommitMessageSanitizer.sanitize(answer));
    }

    @Test
    public void toleratesNullAndBlankAnswers()
    {
        assertNull(CommitMessageSanitizer.sanitize(null));
        assertTrue(CommitMessageSanitizer.sanitize("   \n  ").isBlank());
    }
}
