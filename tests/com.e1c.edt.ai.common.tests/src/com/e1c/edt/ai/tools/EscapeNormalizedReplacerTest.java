package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class EscapeNormalizedReplacerTest
{
    @Test
    public void shouldMatchEscapedNewlineSequence()
    {
        IReplacementStrategy replacer = new EscapeNormalizedReplacer();
        String content = "line1\nline2";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, "line1\\nline2"));

        assertTrue(candidates.size() >= 1);
        assertTrue(candidates.contains("line1\nline2"));
    }

    @Test
    public void shouldReturnNoCandidatesWhenEscapedValueIsNotPresent()
    {
        IReplacementStrategy replacer = new EscapeNormalizedReplacer();

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("A\nB", "A\\tB"));

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void shouldHandleEscapedTabInsideBlock()
    {
        IReplacementStrategy replacer = new EscapeNormalizedReplacer();
        String content = "X\nleft\tright\nY";

        List<String> candidates =
            ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, "left\\tright"));

        assertTrue(candidates.size() >= 1);
        assertTrue(candidates.contains("left\tright"));
    }
}
