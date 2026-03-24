package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class WhitespaceNormalizedReplacerTest
{
    @Test
    public void shouldMatchWhenWhitespaceDiffers()
    {
        IReplacementStrategy replacer = new WhitespaceNormalizedReplacer();

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("A    B", "A B"));

        assertEquals(1, candidates.size());
        assertEquals("A    B", candidates.get(0));
    }

    @Test
    public void shouldReturnNoCandidatesWhenWordsDiffer()
    {
        IReplacementStrategy replacer = new WhitespaceNormalizedReplacer();

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("A B C", "A X C"));

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void shouldMatchMultilineBlockWithNormalizedWhitespace()
    {
        IReplacementStrategy replacer = new WhitespaceNormalizedReplacer();
        String content = "Start\n  one   two\nthree\nEnd";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, "one two\nthree"));

        assertEquals(1, candidates.size());
        assertEquals("  one   two\nthree", candidates.get(0));
    }
}
