package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class BlockAnchorReplacerTest
{
    private final IReplacements replacements = new Replacements();

    @Test
    public void shouldMatchByAnchorsAndMiddleSimilarity()
    {
        IReplacementStrategy replacer = new BlockAnchorReplacer(replacements);
        String content = "Top\nHeader\nline A\nFooter\nBottom";
        String find = "Header\nline B\nFooter";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, find));

        assertEquals(1, candidates.size());
        assertEquals("Header\nline A\nFooter", candidates.get(0));
    }

    @Test
    public void shouldReturnNoCandidatesForBlocksShorterThanThreeLines()
    {
        IReplacementStrategy replacer = new BlockAnchorReplacer(replacements);

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates("A\nB\nC", "A\nB"));

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void shouldPickBestCandidateWhenSeveralAnchorMatchesExist()
    {
        IReplacementStrategy replacer = new BlockAnchorReplacer(replacements);
        String content = "S\nStart\nmiddle first\nEnd\nM\nStart\nmiddle close\nEnd\nE";
        String find = "Start\nmiddle close\nEnd";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, find));

        assertEquals(1, candidates.size());
        assertEquals("Start\nmiddle close\nEnd", candidates.get(0));
    }
}
