package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

@SuppressWarnings("nls")
public class ReplacementsTest
{
    @Test
    public void shouldSplitSingleLine()
    {
        Replacements replacements = new Replacements();
        String[] result = replacements.splitLines("single line");

        assertArrayEquals(new String[] { "single line" }, result);
    }

    @Test
    public void shouldSplitMultipleLines()
    {
        Replacements replacements = new Replacements();
        String[] result = replacements.splitLines("line1\nline2\nline3");

        assertArrayEquals(new String[] { "line1", "line2", "line3" }, result);
    }

    @Test
    public void shouldSplitEmptyString()
    {
        Replacements replacements = new Replacements();
        String[] result = replacements.splitLines("");

        assertArrayEquals(new String[] { "" }, result);
    }

    @Test
    public void shouldHandleTrailingNewline()
    {
        Replacements replacements = new Replacements();
        String[] result = replacements.splitLines("line1\nline2\n");

        assertArrayEquals(new String[] { "line1", "line2", "" }, result);
    }

    @Test
    public void shouldHandleMultipleTrailingNewlines()
    {
        Replacements replacements = new Replacements();
        String[] result = replacements.splitLines("line1\n\n\n");

        assertArrayEquals(new String[] { "line1", "", "", "" }, result);
    }

    @Test
    public void shouldRemoveTrailingEmptyLineWhenPresent()
    {
        Replacements replacements = new Replacements();
        String[] lines = new String[] { "line1", "line2", "" };
        String[] result = replacements.removeTrailingEmptyLine(lines);

        assertArrayEquals(new String[] { "line1", "line2" }, result);
    }

    @Test
    public void shouldNotRemoveWhenNoTrailingEmptyLine()
    {
        Replacements replacements = new Replacements();
        String[] lines = new String[] { "line1", "line2", "line3" };
        String[] result = replacements.removeTrailingEmptyLine(lines);

        assertArrayEquals(lines, result);
    }

    @Test
    public void shouldHandleEmptyArray()
    {
        Replacements replacements = new Replacements();
        String[] lines = new String[0];
        String[] result = replacements.removeTrailingEmptyLine(lines);

        assertArrayEquals(lines, result);
    }

    @Test
    public void shouldHandleSingleEmptyElement()
    {
        Replacements replacements = new Replacements();
        String[] lines = new String[] { "" };
        String[] result = replacements.removeTrailingEmptyLine(lines);

        assertArrayEquals(new String[0], result);
    }

    @Test
    public void shouldExtractBlockByLineRange()
    {
        Replacements replacements = new Replacements();
        String content = "line1\nline2\nline3\nline4\nline5";
        String[] lines = replacements.splitLines(content);
        String result = replacements.blockByLineRange(content, lines, 1, 3);

        assertEquals("line2\nline3\nline4", result);
    }

    @Test
    public void shouldExtractSingleLineBlock()
    {
        Replacements replacements = new Replacements();
        String content = "line1\nline2\nline3";
        String[] lines = replacements.splitLines(content);
        String result = replacements.blockByLineRange(content, lines, 1, 1);

        assertEquals("line2", result);
    }

    @Test
    public void shouldExtractFromFirstLine()
    {
        Replacements replacements = new Replacements();
        String content = "line1\nline2\nline3";
        String[] lines = replacements.splitLines(content);
        String result = replacements.blockByLineRange(content, lines, 0, 2);

        assertEquals("line1\nline2\nline3", result);
    }

    @Test
    public void shouldExtractToLastLine()
    {
        Replacements replacements = new Replacements();
        String content = "line1\nline2\nline3";
        String[] lines = replacements.splitLines(content);
        String result = replacements.blockByLineRange(content, lines, 1, 2);

        assertEquals("line2\nline3", result);
    }

    @Test
    public void shouldHandleBlockWithTrailingNewline()
    {
        Replacements replacements = new Replacements();
        String content = "line1\nline2\nline3\n";
        String[] lines = replacements.splitLines(content);
        String result = replacements.blockByLineRange(content, lines, 0, 1);

        assertEquals("line1\nline2", result);
    }

    @Test
    public void shouldHandleEmptyLinesInRange()
    {
        Replacements replacements = new Replacements();
        String content = "line1\n\nline3\nline4";
        String[] lines = replacements.splitLines(content);
        String result = replacements.blockByLineRange(content, lines, 1, 3);

        assertEquals("\nline3\nline4", result);
    }

    @Test
    public void shouldHandleMultibyteCharacters()
    {
        Replacements replacements = new Replacements();
        String content = "Привет\nМир\nТест";
        String[] lines = replacements.splitLines(content);
        String result = replacements.blockByLineRange(content, lines, 0, 1);

        assertEquals("Привет\nМир", result);
    }

    @Test
    public void shouldHandleMixedLineEndings()
    {
        Replacements replacements = new Replacements();
        String content = "line1\nline2\r\nline3";
        String[] lines = replacements.splitLines(content);
        String result = replacements.blockByLineRange(content, lines, 0, 1);

        assertEquals("line1\nline2\r", result);
    }
}
