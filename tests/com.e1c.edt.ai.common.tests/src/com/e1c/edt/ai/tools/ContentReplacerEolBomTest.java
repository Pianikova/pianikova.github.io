package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

/**
 * Gap-filling tests for {@link ContentReplacer} focused on
 * EOL detection / BOM preservation / line-count corner cases /
 * not-found and multi-match branches not covered by the original
 * {@link ContentReplacerTest}.
 */
@SuppressWarnings("nls")
public class ContentReplacerEolBomTest
{
    private static final String BOM = "﻿";

    private ContentReplacer createContentReplacer()
    {
        IReplacements replacements = new Replacements();
        return new ContentReplacer(Set.of(new SimpleReplacer(), new LineTrimmedReplacer(replacements),
            new BlockAnchorReplacer(replacements), new WhitespaceNormalizedReplacer(replacements),
            new IndentationFlexibleReplacer(replacements), new EscapeNormalizedReplacer(replacements),
            new TrimmedBoundaryReplacer(replacements), new ContextAwareReplacer(replacements),
            new MultiOccurrenceReplacer()));
    }

    // --- not-found / multi-match branches ---

    @Test
    public void testReplaceNotFoundSingleMode()
    {
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result = replacer.replace("Line1\nLine2\nLine3", "missing", "X", "\n", false);

        assertFalse(result.isSuccess());
        assertFalse(result.hasMultipleOccurrences());
        assertEquals("Line1\nLine2\nLine3", result.getUpdatedContent());
        assertEquals(0, result.getAddedLines());
        assertEquals(0, result.getRemovedLines());
    }

    @Test
    public void testReplaceMultipleMatchesWithoutReplaceAll()
    {
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result =
            replacer.replace("aaa\nbbb\nccc\nbbb\nddd", "bbb", "X", "\n", false);

        assertFalse(result.isSuccess());
        assertTrue(result.hasMultipleOccurrences());
        assertEquals("aaa\nbbb\nccc\nbbb\nddd", result.getUpdatedContent());
        assertEquals(0, result.getAddedLines());
        assertEquals(0, result.getRemovedLines());
    }

    // --- line-delimiter detection ---

    @Test
    public void testCrOnlyLineDelimiter()
    {
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result = replacer.replace("Line1\rLine2\rLine3", "Line2", "New", "\r", false);

        assertTrue(result.isSuccess());
        assertEquals("Line1\rNew\rLine3", result.getUpdatedContent());
    }

    @Test
    public void testMixedLineDelimitersPrefersCrlf()
    {
        // Mixed delimiters: detector returns \r\n because crlfCount > 0.
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result =
            replacer.replace("A\r\nB\nC\r\nD", "B", "X", "\n", false);

        assertTrue(result.isSuccess());
        // After normalisation everything became \n; denormalisation maps \n -> \r\n.
        assertEquals("A\r\nX\r\nC\r\nD", result.getUpdatedContent());
    }

    @Test
    public void testSingleLineCurrentFallsBackToProvidedDelimiter()
    {
        // currentContent has no EOL -> detector returns null -> argument is used.
        // The argument matters only when newContent introduces newlines.
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result = replacer.replace("single", "single", "line1\nline2", "\r\n", false);

        assertTrue(result.isSuccess());
        assertEquals("line1\r\nline2", result.getUpdatedContent());
    }

    @Test
    public void testDetectedLineDelimiterOverridesArgument()
    {
        // current is on \n, argument says \r\n -> detector wins, output stays on \n.
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result = replacer.replace("A\nB\nC", "B", "X", "\r\n", false);

        assertTrue(result.isSuccess());
        assertEquals("A\nX\nC", result.getUpdatedContent());
    }

    // --- BOM corner cases ---

    @Test
    public void testBomInOriginContent()
    {
        // BOM appears in originContent only; current has no BOM. stripBOM is applied
        // to all three inputs, so the match must still succeed and the result must
        // not gain a BOM.
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result =
            replacer.replace("hello world", BOM + "hello", "goodbye", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals("goodbye world", result.getUpdatedContent());
        assertFalse("output must not gain a BOM", result.getUpdatedContent().startsWith(BOM));
    }

    @Test
    public void testBomInNewContent()
    {
        // BOM in newContent should be stripped during processing; output must not
        // contain a BOM unless current had one.
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result =
            replacer.replace("hello world", "hello", BOM + "goodbye", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals("goodbye world", result.getUpdatedContent());
        assertFalse(result.getUpdatedContent().startsWith(BOM));
    }

    @Test
    public void testBomWithLfOnly()
    {
        // BOM + LF (no CRLF). BOM must survive.
        ContentReplacer replacer = createContentReplacer();
        String current = BOM + "Line1\nLine2\nLine3";
        ReplaceResult result = replacer.replace(current, "Line2", "X", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals(BOM + "Line1\nX\nLine3", result.getUpdatedContent());
    }

    @Test
    public void testBomPreservedOnNotFound()
    {
        // When nothing matches, ContentReplacer returns the original currentContent
        // verbatim — BOM must therefore still be there.
        ContentReplacer replacer = createContentReplacer();
        String current = BOM + "Line1\nLine2";
        ReplaceResult result = replacer.replace(current, "missing", "X", "\n", false);

        assertFalse(result.isSuccess());
        assertEquals(current, result.getUpdatedContent());
        assertTrue(result.getUpdatedContent().startsWith(BOM));
    }

    // --- line-count corner cases ---

    @Test
    public void testReplaceWithIdenticalOriginAndNewIsNoop()
    {
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result = replacer.replace("L1\nL2\nL3", "L2", "L2", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals("L1\nL2\nL3", result.getUpdatedContent());
        assertEquals(0, result.getAddedLines());
        assertEquals(0, result.getRemovedLines());
    }

    @Test
    public void testReplaceAllMultiplierForRemovedAddedLines()
    {
        // origin = 3 lines collapsed to 1 line (a 2-line removal per occurrence),
        // 2 occurrences in content -> total removed = 4, added = 0.
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result =
            replacer.replace("A\nB\nC\nA\nB\nC", "A\nB\nC", "A", "\n", true);

        assertTrue(result.isSuccess());
        assertTrue(result.hasMultipleOccurrences());
        assertEquals("A\nA", result.getUpdatedContent());
        assertEquals(4, result.getRemovedLines());
        assertEquals(0, result.getAddedLines());
    }

    @Test
    public void testReplaceWithEmptyOriginSingleModeOnNonEmptyContent()
    {
        // replaceAll=false + empty origin -> goes through replaceWithEmptyOrigin's
        // replaceFirst branch. Pattern.quote("") matches the zero-width position
        // before the first character, so newContent is prepended.
        ContentReplacer replacer = createContentReplacer();
        ReplaceResult result = replacer.replace("hello", "", "X", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals("Xhello", result.getUpdatedContent());
        assertFalse(result.hasMultipleOccurrences());
    }
}
