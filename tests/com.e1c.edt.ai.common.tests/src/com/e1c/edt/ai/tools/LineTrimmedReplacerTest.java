package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class LineTrimmedReplacerTest
{
    @Test
    public void shouldMatchIgnoringBoundaryWhitespacePerLine()
    {
        IReplacementStrategy replacer = new LineTrimmedReplacer();
        String content = "A\n  target  \nB";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, "target"));

        assertEquals(1, candidates.size());
        assertEquals("  target  ", candidates.get(0));
    }

    @Test
    public void shouldReturnNoCandidatesWhenTrimmedLinesDoNotMatch()
    {
        IReplacementStrategy replacer = new LineTrimmedReplacer();

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("A\nB\nC", "X\nY"));

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void shouldHandleMultilineBlockWithTrailingNewline()
    {
        IReplacementStrategy replacer = new LineTrimmedReplacer();
        String content = "start\n  one\n two \nend";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, "one\ntwo\n"));

        assertEquals(1, candidates.size());
        assertEquals("  one\n two ", candidates.get(0));
    }
}
