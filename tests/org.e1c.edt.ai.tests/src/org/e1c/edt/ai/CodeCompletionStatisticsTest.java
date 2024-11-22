/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.e1c.edt.ai.assistent.IFeedbackService;
import org.e1c.edt.ai.assistent.model.CursorInfo;
import org.junit.Test;

public class CodeCompletionStatisticsTest
{
    private final IFeedbackService feedbackService = mock(IFeedbackService.class);
    private final ICursorInfoProvider cursorInfoProvider = mock(ICursorInfoProvider.class);
    private final ISource source0 = mock(ISource.class);
    private final static String SOURCE1_ID = "id 1"; //$NON-NLS-1$
    private final ISource source1 = mock(ISource.class);
    private final static String SOURCE2_ID = "id 2"; //$NON-NLS-1$
    private final ISource source2 = mock(ISource.class);
    private static final Optional<CursorInfo> Cursor1 = Optional.of(new CursorInfo());
    private static final Optional<CursorInfo> Cursor2 = Optional.of(new CursorInfo());
    private static final Optional<CursorInfo> Cursor3 = Optional.of(new CursorInfo());
    private static final Optional<CursorInfo> Cursor4 = Optional.of(new CursorInfo());
    private static final Optional<CursorInfo> Cursor5 = Optional.of(new CursorInfo());

    @SuppressWarnings("nls")
    public CodeCompletionStatisticsTest()
    {
        when(source0.getId()).thenReturn("");
        when(source1.getId()).thenReturn(SOURCE1_ID);
        when(source2.getId()).thenReturn(SOURCE2_ID);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldApply()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);
        when(cursorInfoProvider.getCursorInfo(3)).thenReturn(Cursor2);

        // When
        statistics.apply(new Text("Abc", source1), 0);
        statistics.commit("", 0);

        // Then
        verify(feedbackService).acceptedCodeAsync(SOURCE1_ID, "Abc", Cursor1, Cursor2);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldApplyWhenHasInitialOffset()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(10)).thenReturn(Cursor1);
        when(cursorInfoProvider.getCursorInfo(13)).thenReturn(Cursor2);

        // When
        statistics.apply(new Text("Abc", source1), 10);
        statistics.commit("", 0);

        // Then
        verify(feedbackService).acceptedCodeAsync(SOURCE1_ID, "Abc", Cursor1, Cursor2);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldCommitFewTimes()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);
        when(cursorInfoProvider.getCursorInfo(3)).thenReturn(Cursor2);

        // When
        statistics.apply(new Text("Abc", source1), 0);
        statistics.commit("", 0);
        statistics.commit("", 0);

        // Then
        verify(feedbackService, times(1)).acceptedCodeAsync(SOURCE1_ID, "Abc", Cursor1, Cursor2);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldCommitWhenNoCodeWasAccepted()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);

        // When
        statistics.commit(SOURCE1_ID, 0);

        // Then
        verify(feedbackService, times(1)).acceptedCodeAsync(SOURCE1_ID, "", Cursor1, Cursor1);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotCommitWhenNoCodeWasAcceptedTwice()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);

        // When
        statistics.commit(SOURCE1_ID, 0);
        statistics.commit(SOURCE1_ID, 0);
        statistics.commit(SOURCE1_ID, 0);

        // Then
        verify(feedbackService, times(1)).acceptedCodeAsync(SOURCE1_ID, "", Cursor1, Cursor1);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotCommitCursorOffsetIsNegative()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);

        // When
        statistics.commit(SOURCE1_ID, -1);

        // Then
        verify(feedbackService, times(0)).acceptedCodeAsync(SOURCE1_ID, "", Cursor1, Cursor1);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotCommitWhenSourceIdIsEmpty()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);

        // When
        statistics.commit("", 0);

        // Then
        verify(feedbackService, times(0)).acceptedCodeAsync("", "", Cursor1, Cursor1);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotApplyWhenNegativeOffset()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);
        when(cursorInfoProvider.getCursorInfo(3)).thenReturn(Cursor2);

        // When
        statistics.apply(new Text("Abc", source1), -1);
        statistics.commit("", 0);

        // Then
        verify(feedbackService, never()).acceptedCodeAsync(SOURCE1_ID, "Abc", Cursor1, Cursor2);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotApplyWhenSourceIdIsEmpty()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);
        when(cursorInfoProvider.getCursorInfo(3)).thenReturn(Cursor2);

        // When
        statistics.apply(new Text("Abc", source0), -1);
        statistics.commit("", 0);

        // Then
        verify(feedbackService, never()).acceptedCodeAsync("", "Abc", Cursor1, Cursor2);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldApplyFewTimesWhenSameSource()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);
        when(cursorInfoProvider.getCursorInfo(3)).thenReturn(Cursor2);
        when(cursorInfoProvider.getCursorInfo(6)).thenReturn(Cursor3);

        // When
        statistics.apply(new Text("Abc", source1), 0);
        statistics.apply(new Text("Xyz", source1), 3);
        statistics.commit("", 0);

        // Then
        verify(feedbackService).acceptedCodeAsync(SOURCE1_ID, "AbcXyz", Cursor1, Cursor3);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldApplyFewTimesWhenDifSource()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);
        when(cursorInfoProvider.getCursorInfo(3)).thenReturn(Cursor2);
        when(cursorInfoProvider.getCursorInfo(6)).thenReturn(Cursor3);

        // When
        statistics.apply(new Text("Abc", source1), 0);
        statistics.apply(new Text("Xyz", source2), 3);
        statistics.commit("", 0);

        // Then
        verify(feedbackService).acceptedCodeAsync(SOURCE1_ID, "Abc", Cursor1, Cursor2);
        verify(feedbackService).acceptedCodeAsync(SOURCE2_ID, "Xyz", Cursor2, Cursor3);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldApplyWhenMixed()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);
        when(cursorInfoProvider.getCursorInfo(3)).thenReturn(Cursor2);
        when(cursorInfoProvider.getCursorInfo(6)).thenReturn(Cursor3);
        when(cursorInfoProvider.getCursorInfo(12)).thenReturn(Cursor4);

        // When
        statistics.apply(new Text("Abc", source1), 0);
        statistics.apply(new Text("Xyz", source1), 3);
        statistics.apply(new Text("Qwerty", source2), 6);
        statistics.commit("", 0);

        // Then
        verify(feedbackService).acceptedCodeAsync(SOURCE1_ID, "AbcXyz", Cursor1, Cursor3);
        verify(feedbackService).acceptedCodeAsync(SOURCE2_ID, "Qwerty", Cursor3, Cursor4);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldRollback()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);
        when(cursorInfoProvider.getCursorInfo(3)).thenReturn(Cursor2);
        when(cursorInfoProvider.getCursorInfo(6)).thenReturn(Cursor3);
        when(cursorInfoProvider.getCursorInfo(12)).thenReturn(Cursor4);

        // When
        statistics.apply(new Text("Abc", source1), 0);
        statistics.apply(new Text("Xyz", source1), 3);
        statistics.apply(new Text("Qwerty", source2), 6);
        statistics.rollback(12, 6);
        statistics.commit("", 0);

        // Then
        verify(feedbackService).acceptedCodeAsync(SOURCE1_ID, "AbcXyz", Cursor1, Cursor3);
        verify(feedbackService, never()).acceptedCodeAsync(SOURCE2_ID, "Qwerty", Cursor3, Cursor4);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPartialRollback()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);
        when(cursorInfoProvider.getCursorInfo(3)).thenReturn(Cursor2);
        when(cursorInfoProvider.getCursorInfo(6)).thenReturn(Cursor3);
        when(cursorInfoProvider.getCursorInfo(12)).thenReturn(Cursor4);
        when(cursorInfoProvider.getCursorInfo(10)).thenReturn(Cursor5);

        // When
        statistics.apply(new Text("Abc", source1), 0);
        statistics.apply(new Text("Xyz", source1), 3);
        statistics.apply(new Text("Qwerty", source2), 6);
        statistics.rollback(12, 2);
        statistics.commit("", 0);

        // Then
        verify(feedbackService).acceptedCodeAsync(SOURCE1_ID, "AbcXyz", Cursor1, Cursor3);
        verify(feedbackService).acceptedCodeAsync(SOURCE2_ID, "Qwer", Cursor3, Cursor5);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPartialRollbackWhenFewSources()
    {
        // Given
        var statistics = createInstance();
        when(cursorInfoProvider.getCursorInfo(0)).thenReturn(Cursor1);
        when(cursorInfoProvider.getCursorInfo(3)).thenReturn(Cursor2);
        when(cursorInfoProvider.getCursorInfo(6)).thenReturn(Cursor3);
        when(cursorInfoProvider.getCursorInfo(12)).thenReturn(Cursor4);
        when(cursorInfoProvider.getCursorInfo(10)).thenReturn(Cursor5);

        // When
        statistics.apply(new Text("Abc", source1), 0);
        statistics.apply(new Text("Xyz", source1), 3);
        statistics.apply(new Text("Qwerty", source2), 6);
        statistics.rollback(12, 2);
        statistics.rollback(10, 4);
        statistics.rollback(6, 3);
        statistics.commit("", 0);

        // Then
        verify(feedbackService).acceptedCodeAsync(SOURCE1_ID, "Abc", Cursor1, Cursor2);
    }

    private CodeCompletionStatistics createInstance()
    {
        return new CodeCompletionStatistics(feedbackService, cursorInfoProvider);
    }
}
