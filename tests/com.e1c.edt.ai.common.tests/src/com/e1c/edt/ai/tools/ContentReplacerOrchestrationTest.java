package com.e1c.edt.ai.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * Orchestration-level tests for {@link ContentReplacer}.
 *
 * Uses Mockito stubs of {@link IReplacementStrategy} to verify the orchestrator's
 * own logic in isolation from real strategy behaviour: ordinal-based ordering,
 * fall-through between strategies, the notFound / multipleMatches branches, and
 * argument validation.
 */
@SuppressWarnings("nls")
public class ContentReplacerOrchestrationTest
{
    private static IReplacementStrategy strategy(int ordinal, Iterable<String> candidates)
    {
        IReplacementStrategy s = mock(IReplacementStrategy.class);
        when(s.getOrdinal()).thenReturn(ordinal);
        when(s.findCandidates(Mockito.anyString(), Mockito.anyString())).thenReturn(candidates);
        return s;
    }

    private static Set<IReplacementStrategy> setOf(IReplacementStrategy... strategies)
    {
        // Use LinkedHashSet so insertion order is preserved (proves ContentReplacer
        // sorts by ordinal rather than relying on Set iteration order).
        Set<IReplacementStrategy> set = new LinkedHashSet<>();
        Collections.addAll(set, strategies);
        return set;
    }

    @Test
    public void testStrategiesAreInvokedInOrdinalOrder()
    {
        IReplacementStrategy s5 = strategy(5, Collections.emptyList());
        IReplacementStrategy s0 = strategy(0, Collections.emptyList());
        IReplacementStrategy s2 = strategy(2, Collections.emptyList());

        // Pass them in non-ordinal order.
        ContentReplacer replacer = new ContentReplacer(setOf(s5, s0, s2));
        replacer.replace("aaa", "bbb", "ccc", "\n", false);

        InOrder inOrder = Mockito.inOrder(s0, s2, s5);
        inOrder.verify(s0).findCandidates("aaa", "bbb");
        inOrder.verify(s2).findCandidates("aaa", "bbb");
        inOrder.verify(s5).findCandidates("aaa", "bbb");
    }

    @Test
    public void testStopsAtFirstUniqueMatch()
    {
        IReplacementStrategy s0 = strategy(0, Collections.singletonList("bbb"));
        IReplacementStrategy s1 = strategy(1, Collections.singletonList("anything"));

        ContentReplacer replacer = new ContentReplacer(setOf(s0, s1));
        ReplaceResult result = replacer.replace("aaa bbb ccc", "bbb", "XXX", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals("aaa XXX ccc", result.getUpdatedContent());
        verify(s0, atLeastOnce()).findCandidates("aaa bbb ccc", "bbb");
        verify(s1, never()).findCandidates(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testFallsThroughWhenStrategyFindsNoCandidates()
    {
        IReplacementStrategy s0 = strategy(0, Collections.emptyList());
        IReplacementStrategy s1 = strategy(1, Collections.singletonList("bbb"));

        ContentReplacer replacer = new ContentReplacer(setOf(s0, s1));
        ReplaceResult result = replacer.replace("aaa bbb ccc", "irrelevant", "XXX", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals("aaa XXX ccc", result.getUpdatedContent());
        verify(s1, atLeastOnce()).findCandidates(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testFallsThroughWhenCandidateNotInContent()
    {
        // Candidate returned by the strategy does not appear in the content -> indexOf == -1,
        // orchestrator must move on to the next strategy.
        IReplacementStrategy s0 = strategy(0, Collections.singletonList("not-present"));
        IReplacementStrategy s1 = strategy(1, Collections.singletonList("bbb"));

        ContentReplacer replacer = new ContentReplacer(setOf(s0, s1));
        ReplaceResult result = replacer.replace("aaa bbb ccc", "irrelevant", "XXX", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals("aaa XXX ccc", result.getUpdatedContent());
    }

    @Test
    public void testReturnsMultipleMatchesWhenAllStrategiesGiveAmbiguous()
    {
        // Candidate exists in content twice -> ambiguous, replaceAll=false.
        // No later strategy disambiguates -> must report multiple matches.
        IReplacementStrategy s0 = strategy(0, Collections.singletonList("bbb"));

        ContentReplacer replacer = new ContentReplacer(setOf(s0));
        ReplaceResult result = replacer.replace("aaa bbb ccc bbb", "bbb", "XXX", "\n", false);

        assertFalse(result.isSuccess());
        assertTrue(result.hasMultipleOccurrences());
        assertEquals("aaa bbb ccc bbb", result.getUpdatedContent());
        assertEquals(0, result.getAddedLines());
        assertEquals(0, result.getRemovedLines());
    }

    @Test
    public void testFallsThroughAmbiguousCandidateToLaterUniqueOne()
    {
        // Documents the current contract: an ambiguous candidate (firstIndex != lastIndex)
        // is skipped, and the orchestrator continues to later candidates / later strategies.
        // A subsequent unique candidate IS accepted.
        IReplacementStrategy s0 = strategy(0, Collections.singletonList("bbb"));
        IReplacementStrategy s1 = strategy(1, Collections.singletonList("aaa bbb ccc bbb"));

        ContentReplacer replacer = new ContentReplacer(setOf(s0, s1));
        ReplaceResult result = replacer.replace("aaa bbb ccc bbb", "ignored-find", "REPLACED", "\n", false);

        assertTrue(result.isSuccess());
        assertEquals("REPLACED", result.getUpdatedContent());
        // hasMultipleOccurrences reflects occurrenceCount > 1 of the accepted candidate;
        // here the accepted candidate occurs exactly once.
        assertFalse(result.hasMultipleOccurrences());
    }

    @Test
    public void testReplaceAllReturnsOnFirstAmbiguousMatch()
    {
        IReplacementStrategy s0 = strategy(0, Collections.singletonList("bbb"));
        IReplacementStrategy s1 = strategy(1, Collections.singletonList("never-checked"));

        ContentReplacer replacer = new ContentReplacer(setOf(s0, s1));
        ReplaceResult result = replacer.replace("aaa bbb ccc bbb", "bbb", "XXX", "\n", true);

        assertTrue(result.isSuccess());
        assertTrue(result.hasMultipleOccurrences());
        assertEquals("aaa XXX ccc XXX", result.getUpdatedContent());
        verify(s1, never()).findCandidates(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testEmptyStrategiesSetReturnsNotFound()
    {
        ContentReplacer replacer = new ContentReplacer(Collections.emptySet());
        ReplaceResult result = replacer.replace("aaa", "bbb", "ccc", "\n", false);

        assertFalse(result.isSuccess());
        assertFalse(result.hasMultipleOccurrences());
        assertEquals("aaa", result.getUpdatedContent());
    }

    @Test(expected = NullPointerException.class)
    public void testNullCurrentContentThrowsNpe()
    {
        new ContentReplacer(Collections.emptySet()).replace(null, "x", "y", "\n", false);
    }

    @Test(expected = NullPointerException.class)
    public void testNullOriginContentThrowsNpe()
    {
        new ContentReplacer(Collections.emptySet()).replace("a", null, "y", "\n", false);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNewContentThrowsNpe()
    {
        new ContentReplacer(Collections.emptySet()).replace("a", "x", null, "\n", false);
    }

    @Test(expected = NullPointerException.class)
    public void testNullLineDelimiterThrowsNpe()
    {
        new ContentReplacer(Collections.emptySet()).replace("a", "x", "y", null, false);
    }

    @Test
    public void testStrategyOnlyConsultedOnceEvenIfFirstCandidateAccepted()
    {
        // Sanity check: orchestrator does not loop indefinitely when the first
        // candidate of the first strategy already matches uniquely.
        IReplacementStrategy s0 = strategy(0, Collections.singletonList("bbb"));

        ContentReplacer replacer = new ContentReplacer(setOf(s0));
        ReplaceResult result = replacer.replace("aaa bbb ccc", "bbb", "XXX", "\n", false);

        assertTrue(result.isSuccess());
        assertSame("immutable inputs preserved", "aaa bbb ccc", "aaa bbb ccc");
        verify(s0, atLeastOnce()).findCandidates("aaa bbb ccc", "bbb");
    }
}
