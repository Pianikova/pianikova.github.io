package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class ContextAwareReplacerTest
{
    private final IReplacements replacements = new Replacements();

    @Test
    public void shouldMatchUsingContextAnchors()
    {
        IReplacementStrategy replacer = new ContextAwareReplacer(replacements);
        String content = "A\nStart\nsame\nactual\nEnd\nZ";
        String find = "Start\nsame\nexpected\nEnd";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, find));

        assertEquals(1, candidates.size());
        assertEquals("Start\nsame\nactual\nEnd", candidates.get(0));
    }

    @Test
    public void shouldReturnNoCandidatesWhenFindHasLessThanThreeLines()
    {
        IReplacementStrategy replacer = new ContextAwareReplacer(replacements);

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("A\nB\nC", "A\nB"));

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void shouldReturnNoCandidatesWhenSimilarityIsTooLow()
    {
        IReplacementStrategy replacer = new ContextAwareReplacer(replacements);
        String content = "X\nStart\nAAA\nBBB\nEnd\nY";
        String find = "Start\n111\n222\nEnd";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, find));

        assertTrue(candidates.isEmpty());
    }
}
