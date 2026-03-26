package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class IndentationFlexibleReplacerTest
{
    private final IReplacements replacements = new Replacements();

    @Test
    public void shouldMatchWhenOnlyIndentationDiffers()
    {
        IReplacementStrategy replacer = new IndentationFlexibleReplacer(replacements);
        String content = "    line1\n    line2";
        String find = "  line1\n  line2";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, find));

        assertEquals(1, candidates.size());
        assertEquals(content, candidates.get(0));
    }

    @Test
    public void shouldReturnNoCandidatesWhenContentDiffersBeyondIndentation()
    {
        IReplacementStrategy replacer = new IndentationFlexibleReplacer(replacements);
        String content = "if (x) {\n  line1\n}";
        String find = "if (x) {\n  line2\n}";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, find));

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void shouldHandleBlocksWithEmptyLines()
    {
        IReplacementStrategy replacer = new IndentationFlexibleReplacer(replacements);
        String content = "    A\n      one\n\n        two\n    B";
        String find = "  A\n    one\n\n      two\n  B";

        List<String> candidates = ReplacementStrategyTestUtils.toList(replacer.findCandidates(content, find));

        assertEquals(1, candidates.size());
        assertEquals(content, candidates.get(0));
    }
}
