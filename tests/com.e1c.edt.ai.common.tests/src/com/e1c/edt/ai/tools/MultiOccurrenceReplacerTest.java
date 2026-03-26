package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class MultiOccurrenceReplacerTest
{
    @Test
    public void shouldYieldAllExactMatches()
    {
        IReplacementStrategy replacer = new MultiOccurrenceReplacer();

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("foo bar foo baz foo", "foo"));

        assertEquals(3, candidates.size());
        assertEquals("foo", candidates.get(0));
        assertEquals("foo", candidates.get(1));
        assertEquals("foo", candidates.get(2));
    }

    @Test
    public void shouldReturnNoCandidatesWhenNoExactMatchesExist()
    {
        IReplacementStrategy replacer = new MultiOccurrenceReplacer();

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("A B C", "X"));

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void shouldHandleEmptyFindWithoutInfiniteLoop()
    {
        IReplacementStrategy replacer = new MultiOccurrenceReplacer();

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("content", ""));

        assertTrue(candidates.isEmpty());
    }
}
