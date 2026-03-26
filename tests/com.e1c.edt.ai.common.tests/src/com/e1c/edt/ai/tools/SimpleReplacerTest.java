package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class SimpleReplacerTest
{
    @Test
    public void shouldReturnExactFindAsCandidate()
    {
        IReplacementStrategy replacer = new SimpleReplacer();

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("A B C", "B"));

        assertEquals(1, candidates.size());
        assertEquals("B", candidates.get(0));
    }

    @Test
    public void shouldRepresentNoMatchWhenContentDoesNotContainCandidate()
    {
        IReplacementStrategy replacer = new SimpleReplacer();
        String content = "Line1\nLine2";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, "Missing"));

        assertEquals(1, candidates.size());
        assertFalse(content.contains(candidates.get(0)));
    }

    @Test
    public void shouldHandleEmptyFindAsEdgeCase()
    {
        IReplacementStrategy replacer = new SimpleReplacer();

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("content", ""));

        assertEquals(1, candidates.size());
        assertEquals("", candidates.get(0));
    }
}
