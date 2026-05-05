package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

/**
 * Tests for the match-position (line/column) information returned in {@link ReplaceResult}.
 * The position is consumed by {@code EditMcpTool} to render a file link that points at the
 * edited fragment.
 */
@SuppressWarnings("nls")
public class ContentReplacerPositionTest
{
    private static final String BOM = "﻿";

    @Test
    public void testMatchPositionInMiddleOfFile_LF()
    {
        ContentReplacer replacer = createContentReplacer();
        // Lines:
        //   1: "Line1"
        //   2: "Line2"   <-- match starts here, column 1
        //   3: "Line3"
        String currentContent = "Line1\nLine2\nLine3";
        ReplaceResult result = replacer.replace(currentContent, "Line2", "NewLine", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getMatchStartLine());
        assertEquals(1, result.getMatchStartColumn());
        assertEquals(2, result.getMatchEndLine());
        // End column is exclusive — one past the last matched character ("Line2" has 5 chars).
        assertEquals(6, result.getMatchEndColumn());
    }

    @Test
    public void testMatchPositionAtFileStart()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Hello World\nNext line";
        ReplaceResult result = replacer.replace(currentContent, "Hello", "Hi", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMatchStartLine());
        assertEquals(1, result.getMatchStartColumn());
        assertEquals(1, result.getMatchEndLine());
        assertEquals(6, result.getMatchEndColumn());
    }

    @Test
    public void testMatchPositionMidLine()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "alpha beta gamma";
        ReplaceResult result = replacer.replace(currentContent, "beta", "BETA", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMatchStartLine());
        // "alpha " is 6 chars before "beta" → column 7.
        assertEquals(7, result.getMatchStartColumn());
        assertEquals(1, result.getMatchEndLine());
        assertEquals(11, result.getMatchEndColumn());
    }

    @Test
    public void testMatchPositionWithCRLF()
    {
        ContentReplacer replacer = createContentReplacer();
        // CRLF source: line/column must be reported relative to the original text the user sees,
        // independent of the LF normalization done internally.
        String currentContent = "Line1\r\nLine2\r\nLine3";
        ReplaceResult result = replacer.replace(currentContent, "Line2", "NewLine", "\r\n", false);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getMatchStartLine());
        assertEquals(1, result.getMatchStartColumn());
        assertEquals(2, result.getMatchEndLine());
        assertEquals(6, result.getMatchEndColumn());
    }

    @Test
    public void testMatchPositionWithBOM_ColumnExcludesBOM()
    {
        ContentReplacer replacer = createContentReplacer();
        // BOM must not shift the column on line 1.
        String currentContent = BOM + "Hello World";
        ReplaceResult result = replacer.replace(currentContent, "Hello", "Hi", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMatchStartLine());
        assertEquals(1, result.getMatchStartColumn());
        assertEquals(1, result.getMatchEndLine());
        assertEquals(6, result.getMatchEndColumn());
    }

    @Test
    public void testMatchPositionMultilineFragment()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "Line1\nA\nB\nC\nLine5";
        // Fragment "A\nB\nC" spans lines 2-4.
        ReplaceResult result = replacer.replace(currentContent, "A\nB\nC", "X", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getMatchStartLine());
        assertEquals(1, result.getMatchStartColumn());
        assertEquals(4, result.getMatchEndLine());
        // End is one past 'C' on line 4 → column 2.
        assertEquals(2, result.getMatchEndColumn());
    }

    @Test
    public void testMatchPositionSingleLineFile()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "just one line, no newline";
        ReplaceResult result = replacer.replace(currentContent, "one", "ONE", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMatchStartLine());
        assertEquals(6, result.getMatchStartColumn());
        assertEquals(1, result.getMatchEndLine());
        assertEquals(9, result.getMatchEndColumn());
    }

    @Test
    public void testMatchPositionReplaceAll_PointsAtFirstMatch()
    {
        ContentReplacer replacer = createContentReplacer();
        String currentContent = "foo\nbar\nfoo\nbaz";
        ReplaceResult result = replacer.replace(currentContent, "foo", "FOO", "\n", true);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMatchStartLine());
        assertEquals(1, result.getMatchStartColumn());
        assertEquals(1, result.getMatchEndLine());
        assertEquals(4, result.getMatchEndColumn());
    }

    @Test
    public void testMatchPositionEmptyOrigin_PointsAtStart()
    {
        ContentReplacer replacer = createContentReplacer();
        // Empty origin = insertion at very beginning.
        ReplaceResult result = replacer.replace("Hello\nWorld", "", "PREFIX\n", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMatchStartLine());
        assertEquals(1, result.getMatchStartColumn());
        assertEquals(1, result.getMatchEndLine());
        assertEquals(1, result.getMatchEndColumn());
    }

    @Test
    public void testMatchPositionNotFound_IsZero()
    {
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result = replacer.replace("Hello\nWorld", "nonexistent", "x", "\n", false);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getMatchStartLine());
        assertEquals(0, result.getMatchStartColumn());
        assertEquals(0, result.getMatchEndLine());
        assertEquals(0, result.getMatchEndColumn());
    }

    @Test
    public void testMatchPositionMultipleMatches_IsZero()
    {
        ContentReplacer replacer = createContentReplacer();
        // "foo" appears multiple times and replace_all=false → ambiguous, no match position.
        ReplaceResult result = replacer.replace("foo\nfoo\nfoo", "foo", "x", "\n", false);

        assertFalse(result.isSuccess());
        assertTrue(result.hasMultipleOccurrences());
        assertEquals(0, result.getMatchStartLine());
        assertEquals(0, result.getMatchStartColumn());
        assertEquals(0, result.getMatchEndLine());
        assertEquals(0, result.getMatchEndColumn());
    }

    @Test
    public void testMatchPositionWithBOMAndCRLF()
    {
        ContentReplacer replacer = createContentReplacer();
        // BOM + CRLF: position must reference the user-visible coordinates (BOM-stripped, original EOLs).
        String currentContent = BOM + "Header\r\nBody line\r\nFooter";
        ReplaceResult result = replacer.replace(currentContent, "Body line", "Body LINE", "\r\n", false);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getMatchStartLine());
        assertEquals(1, result.getMatchStartColumn());
        assertEquals(2, result.getMatchEndLine());
        assertEquals(10, result.getMatchEndColumn());
    }

    private ContentReplacer createContentReplacer()
    {
        IReplacements replacements = new Replacements();
        return new ContentReplacer(Set.of(new SimpleReplacer(), new LineTrimmedReplacer(replacements),
            new BlockAnchorReplacer(replacements), new WhitespaceNormalizedReplacer(replacements),
            new IndentationFlexibleReplacer(replacements), new EscapeNormalizedReplacer(replacements),
            new TrimmedBoundaryReplacer(replacements), new ContextAwareReplacer(replacements),
            new MultiOccurrenceReplacer()));
    }
}
