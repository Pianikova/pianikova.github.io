/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

/**
 * Tests for MarkdownUtils diff rendering.
 */
@SuppressWarnings("nls")
public class MarkdownUtilsDiffTest
{
    private static final String PATH = "C:/project/src/Module.bsl";

    private final IFiles files = mock(IFiles.class);
    private final MarkdownUtils markdownUtils = new MarkdownUtils(new LinkProvider(), files);

    @Test
    public void buildGitDiffAddsClickableLineNumbers()
    {
        var diff = markdownUtils.buildGitDiff(PATH, "line 1\nline 2\nline 3", "line 1\nline two\nline 3");

        assertTrue(diff.contains("href=\"edt-file://C%3A/project/src/Module.bsl:2:1\""));
        assertTrue(diff.contains(">2</a>"));
        assertFalse(diff.contains("> 2</a>"));
        assertTrue(diff.contains("-line 2"));
        assertTrue(diff.contains("+line two"));
    }

    @Test
    public void buildGitDiffUsesProvidedStartLinesForFragmentDiff()
    {
        var diff = markdownUtils.buildGitDiff(PATH, "same line\nold line", "same line\nnew line", 22, 22);

        assertTrue(diff.contains("href=\"edt-file://C%3A/project/src/Module.bsl:22:1\""));
        assertTrue(diff.contains("href=\"edt-file://C%3A/project/src/Module.bsl:23:1\""));
        assertFalse(diff.contains("href=\"edt-file://C%3A/project/src/Module.bsl:1:1\""));
    }

    @Test
    public void buildGitDiffShowsInsertedLinesInsideMergedHunk()
    {
        var origin = String.join("\n", "Функция Факториал(Число) Экспорт", "Если Число < 0 Тогда", "Возврат 0;",
            "КонецЕсли;");
        var changed = String.join("\n", "Функция Факториал(Число) Экспорт", "// Проверка на отрицательное число",
            "Если Число < 0 Тогда", "Возврат 0;", "КонецЕсли;");

        var diff = markdownUtils.buildGitDiff(PATH, origin, changed, 22, 22);

        assertTrue(diff.contains("+// Проверка на отрицательное число"));
        assertTrue(diff.contains("href=\"edt-file://C%3A/project/src/Module.bsl:23:1\""));
    }

    @Test
    public void buildGitDiffCanRenderLineNumbersBeforeEditing()
    {
        var origin = String.join("\n", "line 22", "line 23", "line 24");
        var changed = String.join("\n", "line 22", "inserted line", "line 23", "line 24");

        var diff = markdownUtils.buildGitDiff(PATH, origin, changed, 22, 22, false);

        assertTrue(diff.contains("+inserted line"));
        assertTrue(diff.contains("href=\"edt-file://C%3A/project/src/Module.bsl:23:1\">23</a>  line 23"));
        assertFalse(diff.contains("href=\"edt-file://C%3A/project/src/Module.bsl:24:1\">24</a>  line 23"));
    }

    @Test
    public void buildGitDiffCanRenderLineNumbersAfterEditing()
    {
        var origin = String.join("\n", "line 22", "line 23", "line 24");
        var changed = String.join("\n", "line 22", "inserted line", "line 23", "line 24");

        var diff = markdownUtils.buildGitDiff(PATH, origin, changed, 22, 22, true);

        assertTrue(diff.contains("+inserted line"));
        assertTrue(diff.contains("href=\"edt-file://C%3A/project/src/Module.bsl:24:1\">24</a>  line 23"));
    }

    @Test
    public void buildGitDiffExpandsLineNumberColumnForLargeLinesWithoutClickablePadding()
    {
        var diff = markdownUtils.buildGitDiff(PATH, "line 1000\nold line", "line 1000\nnew line", 1000, 1000);

        assertTrue(diff.contains("href=\"edt-file://C%3A/project/src/Module.bsl:1001:1\">1001</a>"));
        assertFalse(diff.contains("> 1001</a>"));
    }

    @Test
    public void buildGitDiffDoesNotRenderFutureChangesAsGrayContext()
    {
        var origin = String.join("\n", "line 1", "line 2 old", "shared middle", "line 4 old", "line 5");
        var changed = String.join("\n", "line 1", "line 2 new", "shared middle", "line 4 new", "line 5");

        var diff = markdownUtils.buildGitDiff(PATH, origin, changed);

        assertEquals(1, occurrences(diff, "line 4 old"));
        assertTrue(diff.contains("-line 4 old"));
        assertTrue(diff.contains("+line 4 new"));
        assertFalse(diff.contains(" line 4 old"));
    }

    @Test
    public void buildUnifiedDiffByFileAddsClickableLineNumbersFromHunkHeader()
    {
        var diffText = String.join("\n", "diff --git a/src/Module.bsl b/src/Module.bsl", "index 123..456 100644",
            "--- a/src/Module.bsl", "+++ b/src/Module.bsl", "@@ -10,3 +10,3 @@", " context",
            "-old value", "+new value");

        var diff = markdownUtils.buildUnifiedDiffByFile(diffText);

        assertTrue(diff.contains("href=\"edt-file://src/Module.bsl:10:1\""));
        assertTrue(diff.contains("href=\"edt-file://src/Module.bsl:11:1\""));
        assertTrue(diff.contains("@@ -10,3 +10,3 @@"));
        assertTrue(diff.contains("-old value"));
        assertTrue(diff.contains("+new value"));
    }

    private static int occurrences(String text, String value)
    {
        var count = 0;
        var index = 0;
        while ((index = text.indexOf(value, index)) >= 0)
        {
            count++;
            index += value.length();
        }
        return count;
    }
}
