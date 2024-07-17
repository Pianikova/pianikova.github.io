/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.e1c.edt.ai.CodeCompletionAction;
import org.e1c.edt.ai.CodeCompletionSession;
import org.e1c.edt.ai.HintPart;
import org.e1c.edt.ai.ICodeCompletionContext;
import org.e1c.edt.ai.IHintHistory;
import org.e1c.edt.ai.IHistoricalHint;
import org.e1c.edt.ai.ISource;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.Text;
import org.junit.Assert;
import org.junit.Test;

public class CodeCompletionSessionTest
{
    private final IUISettings uiSettings = mock(IUISettings.class);
    private final IHistoricalHint hint = mock(IHistoricalHint.class);
    private final IHintHistory history = mock(IHintHistory.class);
    private final ICodeCompletionContext context = mock(ICodeCompletionContext.class);
    private final ISource source = mock(ISource.class);

    @Test
    public void shouldProvideContext()
    {
        // Given
        var session = createInstance(false);

        // When
        var actual = session.getContext();

        // Then
        Assert.assertEquals(context, actual);
    }

    @Test
    public void shouldProvideHint()
    {
        // Given
        var session = createInstance(false);

        // When
        var actual = session.getHint();

        // Then
        Assert.assertEquals(hint, actual);
    }

    @Test
    public void shouldBeDoneWhenCompletedAndEmpty()
    {
        // Given
        var session = createInstance(false);

        // When
        session.complete();
        when(hint.isEmpty()).thenReturn(true);
        var actual = session.isDone();

        // Then
        Assert.assertTrue(actual);
    }

    @Test
    public void shouldBeDoneWhenCompletedAndNotEmpty()
    {
        // Given
        var session = createInstance(false);

        // When
        session.complete();
        when(hint.isEmpty()).thenReturn(false);
        var actual = session.isDone();

        // Then
        Assert.assertFalse(actual);
    }

    @Test
    public void shouldBeDoneWhenNotCompletedAndEmpty()
    {
        // Given
        var session = createInstance(false);

        // When
        when(hint.isEmpty()).thenReturn(true);
        var actual = session.isDone();

        // Then
        Assert.assertFalse(actual);
    }

    @Test
    public void shouldReturnRESETWhenFinish()
    {
        // Given
        var session = createInstance(false);

        // When
        var actualAction = session.finish();

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAccept()
    {
        // Given
        var session = createInstance(false);
        when(hint.isEmpty()).thenReturn(false);
        when(hint.pull(HintPart.TOKEN)).thenReturn(new Text("Abc", source));

        // When
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).replace(37, 0, "Abc");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldSetIsAcceptingDuringAccept()
    {
        // Given
        var session = createInstance(false);
        when(hint.isEmpty()).thenReturn(false);
        when(hint.pull(HintPart.TOKEN)).thenReturn(new Text("Abc", source));
        doAnswer(i -> {
            Assert.assertTrue(session.isAccepting());
            return null;
        }).when(context).replace(37, 0, "Abc");

        // When
        session.accept(HintPart.TOKEN, 37);

        // Then
    }

    @Test
    public void shouldNotAcceptAndReturnSKIPWhenHintIsEmptyAndHistoryIsEmpty()
    {
        // Given
        var session = createInstance(false);

        // When
        when(hint.isEmpty()).thenReturn(true);
        when(history.isEmpty()).thenReturn(true);
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.SKIP, actualAction);
        verify(context, never()).replace(anyInt(), anyInt(), anyString());
    }

    @Test
    public void shouldNotAcceptAndReturnHANDLEWhenHintIsEmptyAndHistoryIsNotEmpty()
    {
        // Given
        var session = createInstance(false);

        // When
        when(hint.isEmpty()).thenReturn(true);
        when(history.isEmpty()).thenReturn(false);
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.HANDLE, actualAction);
        verify(context, never()).replace(anyInt(), anyInt(), anyString());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnRESETForAcceptWhenSingleWordModeAndHintIsBlank()
    {
        // Given
        var session = createInstance(true);

        // When
        when(hint.isEmpty()).thenReturn(false);
        when(hint.isBlank()).thenReturn(true);
        when(hint.pull(HintPart.TOKEN)).thenReturn(new Text("Abc", source));
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).replace(37, 0, "Abc");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnRESETForAcceptWhenSingleWordModeAndHintIsBlankAndHintStartsWithNewLine()
    {
        // Given
        var session = createInstance(true);
        when(hint.pull(HintPart.TOKEN)).thenReturn(new Text("Abc", source));

        // When
        when(hint.isEmpty()).thenReturn(false);
        when(hint.isBlank()).thenReturn(true);
        when(hint.startsWith('\n')).thenReturn(true);
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).replace(37, 0, "Abc");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnRESETForAcceptWhenSingleWordModeAndHintIsNotBlankAndHintStartsWithNewLine()
    {
        // Given
        var session = createInstance(true);
        when(hint.pull(HintPart.TOKEN)).thenReturn(new Text("Abc", source));

        // When
        when(hint.isEmpty()).thenReturn(false);
        when(hint.isBlank()).thenReturn(true);
        when(hint.startsWith('\n')).thenReturn(false);
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).replace(37, 0, "Abc");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnUPDATEForAcceptWhenNotSingleWordModeHintIsBlankAndHintStartsWithNewLine()
    {
        // Given
        var session = createInstance(false);
        when(hint.pull(HintPart.TOKEN)).thenReturn(new Text("Abc", source));

        // When
        when(hint.isEmpty()).thenReturn(false);
        when(hint.isBlank()).thenReturn(true);
        when(hint.startsWith('\n')).thenReturn(true);
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).replace(37, 0, "Abc");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAcceptChar()
    {
        // Given
        var session = createInstance(false);
        when(hint.isEmpty()).thenReturn(false);
        when(hint.pullChar('A')).thenReturn(new Text("A", source));

        // When
        var actualAction = session.acceptChar(37, 'A');

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).replace(37, 0, "A");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnRESETWhenAcceptCharAndDoesNotPullChar()
    {
        // Given
        var session = createInstance(false);

        // When
        var actualAction = session.acceptChar(37, '\b');

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context, times(0)).replace(37, 0, "\b");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnASK_NEWWhenAcceptCharAndDoesNotPullChar()
    {
        // Given
        var session = createInstance(false);
        when(hint.isEmpty()).thenReturn(false);
        when(hint.pullChar('A')).thenReturn(new Text("", source));

        // When
        var actualAction = session.acceptChar(37, 'A');

        // Then
        Assert.assertEquals(CodeCompletionAction.ASK_NEW, actualAction);
        verify(context, times(0)).replace(37, 0, "A");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnRESETForAcceptCharWhenSingleWordModeAndHintIsBlank()
    {
        // Given
        var session = createInstance(true);
        when(hint.pullChar('A')).thenReturn(new Text("A", source));

        // When
        when(hint.isEmpty()).thenReturn(false);
        when(hint.isBlank()).thenReturn(true);
        var actualAction = session.acceptChar(37, 'A');

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).replace(37, 0, "A");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnRESETForAcceptCharWhenSingleWordModeAndHintIsBlankAndHintStartsWithNewLine()
    {
        // Given
        var session = createInstance(true);
        when(hint.pullChar('A')).thenReturn(new Text("A", source));

        // When
        when(hint.isEmpty()).thenReturn(false);
        when(hint.isBlank()).thenReturn(true);
        when(hint.startsWith('\n')).thenReturn(true);
        var actualAction = session.acceptChar(37, 'A');

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).replace(37, 0, "A");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnRESETForAcceptCharWhenSingleWordModeAndHintIsNotBlankAndHintStartsWithNewLine()
    {
        // Given
        var session = createInstance(true);
        when(hint.pullChar('A')).thenReturn(new Text("A", source));

        // When
        when(hint.isEmpty()).thenReturn(false);
        when(hint.isBlank()).thenReturn(true);
        when(hint.startsWith('\n')).thenReturn(false);
        var actualAction = session.acceptChar(37, 'A');

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).replace(37, 0, "A");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnUPDATEForAcceptCharWhenNotSingleWordModeHintIsBlankAndHintStartsWithNewLine()
    {
        // Given
        var session = createInstance(false);
        when(hint.pullChar('A')).thenReturn(new Text("A", source));

        // When
        when(hint.isEmpty()).thenReturn(false);
        when(hint.isBlank()).thenReturn(true);
        when(hint.startsWith('\n')).thenReturn(true);
        var actualAction = session.acceptChar(37, 'A');

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).replace(37, 0, "A");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldRollback()
    {
        // Given
        var session = createInstance(false);
        when(hint.rollback()).thenReturn(new Text("Abc", source));

        // When
        var actualAction = session.rollback(37);

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).replace(34, 3, "");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldSetIsAcceptingDuringRollback()
    {
        // Given
        var session = createInstance(false);
        when(hint.rollback()).thenReturn(new Text("Abc", source));
        doAnswer(i -> {
            Assert.assertTrue(session.isAccepting());
            return null;
        }).when(context).replace(34, 3, "");

        // When
        session.rollback(37);

        // Then
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotRollbackWhenHintDoesNotRollback()
    {
        // Given
        var session = createInstance(false);
        when(hint.rollback()).thenReturn(new Text("", source));

        // When
        var actualAction = session.rollback(37);

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context, never()).replace(anyInt(), anyInt(), anyString());
    }

    @Test
    public void shouldReset()
    {
        // Given
        var session = createInstance(false);
        when(hint.isEmpty()).thenReturn(false);
        session.complete();

        // When
        session.reset();

        // Then
        Assert.assertFalse(session.isDone());
        verify(hint).clear();
    }

    private CodeCompletionSession<ICodeCompletionContext> createInstance(boolean isSingleWordMode)
    {
        var session = new CodeCompletionSession<>(uiSettings, hint, history);
        session.initiaize(context, history, isSingleWordMode);
        return session;
    }
}
