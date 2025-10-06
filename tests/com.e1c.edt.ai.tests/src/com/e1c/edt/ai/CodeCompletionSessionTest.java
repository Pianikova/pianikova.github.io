/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;

public class CodeCompletionSessionTest
{
    private final ISettings settings = mock(ISettings.class);
    private final IHistoricalHint hint = mock(IHistoricalHint.class);
    private final IHintHistory history = mock(IHintHistory.class);
    private final ICodeCompletionContext context = mock(ICodeCompletionContext.class);
    private final ISource source = mock(ISource.class);
    private final Text TEXT = new Text("Abc", source); //$NON-NLS-1$

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

    @Test
    public void shouldAccept()
    {
        // Given
        var session = createInstance(false);
        when(hint.isEmpty()).thenReturn(false);
        when(hint.pull(HintPart.TOKEN)).thenReturn(TEXT);

        // When
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).apply(TEXT, 37);
    }

    @Test
    public void shouldSetIsAcceptingDuringAccept()
    {
        // Given
        var session = createInstance(false);
        when(hint.isEmpty()).thenReturn(false);
        when(hint.pull(HintPart.TOKEN)).thenReturn(TEXT);
        doAnswer(i -> {
            Assert.assertTrue(session.isAccepting());
            return null;
        }).when(context).apply(TEXT, 37);

        // When
        session.accept(HintPart.TOKEN, 37);

        // Then
    }

    @Test
    public void shouldNotAcceptAndReturnRESETWhenHintIsEmptyAndHistoryIsEmpty()
    {
        // Given
        var session = createInstance(false);

        // When
        when(hint.isEmpty()).thenReturn(true);
        when(history.isEmpty()).thenReturn(true);
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context, never()).apply(any(), anyInt());
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
        verify(context, never()).apply(any(), anyInt());
    }

    @Test
    public void shouldReturnRESETForAcceptWhenSingleWordModeAndHintIsEmpty()
    {
        // Given
        var session = createInstance(true);

        // When
        when(hint.isEmpty()).thenReturn(false).thenReturn(true);
        when(hint.isBlank()).thenReturn(true);
        when(hint.pull(HintPart.TOKEN)).thenReturn(TEXT);
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).apply(TEXT, 37);
    }

    @Test
    public void shouldReturnRESETForAcceptWhenSingleWordModeAndHintIsEmptyAndHintStartsWithNewLine()
    {
        // Given
        var session = createInstance(true);
        when(hint.pull(HintPart.TOKEN)).thenReturn(TEXT);

        // When
        when(hint.isEmpty()).thenReturn(false).thenReturn(true);
        when(hint.isBlank()).thenReturn(true);
        when(hint.startsWith('\n')).thenReturn(true);
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).apply(TEXT, 37);
    }

    @Test
    public void shouldReturnRESETForAcceptWhenSingleWordModeAndHintIsNotEmptyAndHintStartsWithNewLine()
    {
        // Given
        var session = createInstance(true);
        when(hint.pull(HintPart.TOKEN)).thenReturn(TEXT);

        // When
        when(hint.isEmpty()).thenReturn(false).thenReturn(true);
        when(hint.isBlank()).thenReturn(true);
        when(hint.startsWith('\n')).thenReturn(false);
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).apply(TEXT, 37);
    }

    @Test
    public void shouldReturnUPDATEForAcceptWhenNotSingleWordModeHintIsBlankAndHintStartsWithNewLine()
    {
        // Given
        var session = createInstance(false);
        when(hint.pull(HintPart.TOKEN)).thenReturn(TEXT);

        // When
        when(hint.isEmpty()).thenReturn(false);
        when(hint.isBlank()).thenReturn(true);
        when(hint.startsWith('\n')).thenReturn(true);
        var actualAction = session.accept(HintPart.TOKEN, 37);

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).apply(TEXT, 37);
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
        verify(context).apply(new Text("A", source), 37);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAcceptLineSeparatorWhenCariageReturnWhenWindows()
    {
        // Given
        var session = createInstance(false);
        when(settings.getLineSeparator()).thenReturn("\r\n");
        when(hint.isEmpty()).thenReturn(false);
        when(hint.pullChar(null)).thenReturn(new Text("\r", source))
            .thenReturn(new Text("\n", source))
            .thenReturn(new Text("Abc", source));

        // When
        var actualAction = session.acceptChar(37, '\r');

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).apply(new Text("\r\n", source), 37);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAcceptLineSeparatorWhenCariageReturnWhenMac()
    {
        // Given
        var session = createInstance(false);
        when(hint.isEmpty()).thenReturn(false);
        when(hint.pullChar(null)).thenReturn(new Text("\r", source)).thenReturn(new Text("Abc", source));

        // When
        var actualAction = session.acceptChar(37, '\r');

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).apply(new Text("\r", source), 37);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAcceptUntilNextNotEmptySymbolWhenCariageReturn()
    {
        // Given
        var session = createInstance(false);
        when(hint.isEmpty()).thenReturn(false);
        when(hint.pullChar(null)).thenReturn(new Text("\r", source))
            .thenReturn(new Text("\n", source))
            .thenReturn(new Text("\t", source))
            .thenReturn(new Text(" ", source))
            .thenReturn(new Text("\t", source))
            .thenReturn(new Text("Abc", source));

        // When
        var actualAction = session.acceptChar(37, '\r');

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).apply(new Text("\r\n\t \t", source), 37);
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
        verify(context, never()).apply(new Text("\b", source), 37);
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
        verify(context, never()).apply(new Text("A", source), 37);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnRESETForAcceptCharWhenSingleWordModeAndHintIsEmpty()
    {
        // Given
        var session = createInstance(true);
        when(hint.pullChar('A')).thenReturn(new Text("A", source));

        // When
        when(hint.isEmpty()).thenReturn(true);
        when(hint.isBlank()).thenReturn(false);
        var actualAction = session.acceptChar(37, 'A');

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).apply(new Text("A", source), 37);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnRESETForAcceptCharWhenSingleWordModeAndHintIsEmptyAndHintStartsWithNewLine()
    {
        // Given
        var session = createInstance(true);
        when(hint.pullChar('A')).thenReturn(new Text("A", source));

        // When
        when(hint.isEmpty()).thenReturn(true);
        when(hint.isBlank()).thenReturn(true);
        when(hint.startsWith('\n')).thenReturn(true);
        var actualAction = session.acceptChar(37, 'A');

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).apply(new Text("A", source), 37);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldReturnRESETForAcceptCharWhenSingleWordModeAndHintIsNotEmptyAndHintStartsWithNewLine()
    {
        // Given
        var session = createInstance(true);
        when(hint.pullChar('A')).thenReturn(new Text("A", source));

        // When
        when(hint.isEmpty()).thenReturn(true);
        when(hint.isBlank()).thenReturn(true);
        when(hint.startsWith('\n')).thenReturn(false);
        var actualAction = session.acceptChar(37, 'A');

        // Then
        Assert.assertEquals(CodeCompletionAction.RESET, actualAction);
        verify(context).apply(new Text("A", source), 37);
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
        verify(context).apply(new Text("A", source), 37);
    }

    @Test
    public void shouldRollback()
    {
        // Given
        var session = createInstance(false);
        when(hint.rollback()).thenReturn(TEXT);

        // When
        var actualAction = session.rollback(37);

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).rollback(34, 3);
    }

    @Test
    public void shouldRollbackNewLineWhenWindows()
    {
        // Given
        var session = createInstance(false);
        var newLine = new Text("\n", source); //$NON-NLS-1$
        when(settings.getLineSeparator()).thenReturn("\r\n"); //$NON-NLS-1$
        when(hint.rollback()).thenReturn(newLine);

        // When
        var actualAction = session.rollback(37);

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).rollback(35, 2);
    }

    @Test
    public void shouldRollbackNewLineWhenLinux()
    {
        // Given
        var session = createInstance(false);
        var newLine = new Text("\n", source); //$NON-NLS-1$
        when(settings.getLineSeparator()).thenReturn("\n"); //$NON-NLS-1$
        when(hint.rollback()).thenReturn(newLine);

        // When
        var actualAction = session.rollback(37);

        // Then
        Assert.assertEquals(CodeCompletionAction.UPDATE, actualAction);
        verify(context).rollback(36, 1);
    }

    @Test
    public void shouldSetIsAcceptingDuringRollback()
    {
        // Given
        var session = createInstance(false);
        when(hint.rollback()).thenReturn(TEXT);
        doAnswer(i -> {
            Assert.assertTrue(session.isAccepting());
            return null;
        }).when(context).rollback(34, 3);

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
        verify(context, never()).rollback(anyInt(), anyInt());
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
        var session = new CodeCompletionSession<>(settings, hint, history);
        session.initiaize(context, history, 3, isSingleWordMode);
        return session;
    }
}
