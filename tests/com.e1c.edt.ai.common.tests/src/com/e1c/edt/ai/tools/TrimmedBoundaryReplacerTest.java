package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class TrimmedBoundaryReplacerTest
{
    @Test
    public void shouldMatchWhenFindHasOuterSpaces()
    {
        IReplacementStrategy replacer = new TrimmedBoundaryReplacer();
        String content = "before target after";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, "  target  "));

        assertEquals(1, candidates.size());
        assertEquals("target", candidates.get(0));
    }

    @Test
    public void shouldReturnNoCandidatesWhenFindAlreadyTrimmed()
    {
        IReplacementStrategy replacer = new TrimmedBoundaryReplacer();

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("target", "target"));

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void shouldMatchTrimmedMultilineBlock()
    {
        IReplacementStrategy replacer = new TrimmedBoundaryReplacer();
        String content = "A\n  block line\n  next line\nB";
        String find = "  block line\n  next line\n";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, find));

        assertEquals(1, candidates.size());
        assertEquals("block line\n  next line", candidates.get(0));
    }
}
