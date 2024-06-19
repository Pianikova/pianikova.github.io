/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.e1c.edt.ai.CodeCompletionToken;
import org.e1c.edt.ai.Hint;
import org.e1c.edt.ai.HintPart;
import org.e1c.edt.ai.ICodeCompletionTokenizer;
import org.e1c.edt.ai.IHintHistory;
import org.junit.Assert;
import org.junit.Test;

public class HintTest
{
    private final ICodeCompletionTokenizer tokenizer = mock(ICodeCompletionTokenizer.class);
    private final IHintHistory history = mock(IHintHistory.class);

    @Test
    public void shouldBeEmptyWhenCreated()
    {
        // Given

        // When
        var hint = createInstance();

        // Then
        Assert.assertTrue(hint.isEmpty());
        Assert.assertTrue(hint.isBlank());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAppendText()
    {
        // Given
        var hint = createInstance();

        // When
        hint.append("Abc");
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertFalse(hint.isEmpty());
        Assert.assertFalse(hint.isBlank());
        Assert.assertEquals("Abc", actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotBeEmptyWhenHasText()
    {
        // Given
        var hint = createInstance();

        // When
        hint.append("Abc");

        // Then
        Assert.assertFalse(hint.isEmpty());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldBeEmptyWhenHasNoText()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");

        // When
        hint.clear();

        // Then
        Assert.assertTrue(hint.isEmpty());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotBeBlankWhenHasText()
    {
        // Given
        var hint = createInstance();

        // When
        hint.append("Abc");

        // Then
        Assert.assertFalse(hint.isBlank());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldBeBlankWhenHasNoText()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");

        // When
        hint.clear();

        // Then
        Assert.assertTrue(hint.isBlank());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldBeBlankWhenHasBlankText()
    {
        // Given
        var hint = createInstance();

        // When
        hint.append("  \r   \n  \t\t\r");

        // Then
        Assert.assertTrue(hint.isBlank());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldCheckWhenStartWith()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");

        // When
        var actualResult = hint.startsWith('A');

        // Then
        Assert.assertTrue(actualResult);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldCheckWhenDoesNotStartWith()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");

        // When
        var actualResult = hint.startsWith('B');

        // Then
        Assert.assertFalse(actualResult);
    }

    @Test
    public void shouldCheckWhenDoesNotStartWithAndEmpty()
    {
        // Given
        var hint = createInstance();

        // When
        var actualResult = hint.startsWith('A');

        // Then
        Assert.assertFalse(actualResult);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetText()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");

        // When
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertEquals("Abc", actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetTextWhenEmpty()
    {
        // Given
        var hint = createInstance();

        // When
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertEquals("", actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullText()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");

        // When
        var actualText = hint.pull(HintPart.TEXT);

        // Then
        Assert.assertEquals("Abc", actualText);
        verify(history).push("Abc");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetToken()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");
        when(tokenizer.getNext(1, "Abc", Hint.TOKEN_DELIMITER)).thenReturn(new CodeCompletionToken("Ab", "c"));

        // When
        var actualText = hint.getText(HintPart.TOKEN);

        // Then
        Assert.assertEquals("Ab", actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetTokenWhenEmpty()
    {
        // Given
        var hint = createInstance();

        // When
        var actualText = hint.getText(HintPart.TOKEN);

        // Then
        Assert.assertEquals("", actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullToken()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");
        when(tokenizer.getNext(1, "Abc", Hint.TOKEN_DELIMITER)).thenReturn(new CodeCompletionToken("Ab", "c"));

        // When
        var actualText = hint.pull(HintPart.TOKEN);

        // Then
        Assert.assertEquals("Ab", actualText);
        verify(history).push("Ab");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetLines()
    {
        // Given
        var hint = createInstance();
        hint.setMaxLines(5);
        hint.append("Abc|Xyz|Asd");
        when(tokenizer.getNext(1, "Abc|Xyz|Asd", Hint.LINE_DELIMITER))
            .thenReturn(new CodeCompletionToken("Abc|", "Xyz|Asd"));
        when(tokenizer.getNext(1, "Xyz|Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Xyz|", "Asd"));
        when(tokenizer.getNext(1, "Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Asd", ""));

        // When
        var actualText = hint.getText(HintPart.LINES);

        // Then
        Assert.assertEquals("Abc|Xyz|Asd", actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetLinesWhenEmpty()
    {
        // Given
        var hint = createInstance();
        hint.setMaxLines(5);

        // When
        var actualText = hint.getText(HintPart.LINES);

        // Then
        Assert.assertEquals("", actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullLines()
    {
        // Given
        var hint = createInstance();
        hint.setMaxLines(5);
        hint.append("Abc|Xyz|Asd");
        when(tokenizer.getNext(1, "Abc|Xyz|Asd", Hint.LINE_DELIMITER))
            .thenReturn(new CodeCompletionToken("Abc|", "Xyz|Asd"));
        when(tokenizer.getNext(1, "Xyz|Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Xyz|", "Asd"));
        when(tokenizer.getNext(1, "Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Asd", ""));

        // When
        var actualText = hint.pull(HintPart.LINES);

        // Then
        Assert.assertEquals("Abc|Xyz|Asd", actualText);
        verify(history).push("Abc|Xyz|Asd");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetLinesWhenLimitedByMaxLines()
    {
        // Given
        var hint = createInstance();
        hint.setMaxLines(2);
        hint.append("Abc|Xyz|Asd");
        when(tokenizer.getNext(1, "Abc|Xyz|Asd", Hint.LINE_DELIMITER))
            .thenReturn(new CodeCompletionToken("Abc|", "Xyz|Asd"));
        when(tokenizer.getNext(1, "Xyz|Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Xyz|", "Asd"));

        // When
        var actualText = hint.getText(HintPart.LINES);

        // Then
        Assert.assertEquals("Abc|Xyz|", actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullCharWhenMatch()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");

        // When
        var actualText = hint.pullChar('A');

        // Then
        Assert.assertEquals("A", actualText);
        verify(history).push("A");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotPullCharWhenNotMatch()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");

        // When
        var actualText = hint.pullChar('b');

        // Then
        Assert.assertEquals("", actualText);
        verify(history, times(0)).push(any());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotPullCharWhenEmpty()
    {
        // Given
        var hint = createInstance();

        // When
        var actualText = hint.pullChar('A');

        // Then
        Assert.assertEquals("", actualText);
        verify(history, times(0)).push(any());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldClear()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");
        hint.append("Xyz");

        // When
        hint.clear();

        // Then
        Assert.assertTrue(hint.isEmpty());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldRollback()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");
        hint.append(" Xyz");
        when(tokenizer.getNext(1, "Abc Xyz", Hint.TOKEN_DELIMITER)).thenReturn(new CodeCompletionToken("Abc ", "Xyz"));
        hint.pull(HintPart.TOKEN);
        when(history.pull()).thenReturn("Asd ");

        // When
        hint.rollback();
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertEquals("Asd Xyz", actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotRollbackWhenHistoryIsEmpty()
    {
        // Given
        var hint = createInstance();
        hint.append("Abc");
        hint.append(" Xyz");
        when(tokenizer.getNext(1, "Abc Xyz", Hint.TOKEN_DELIMITER)).thenReturn(new CodeCompletionToken("Abc ", "Xyz"));
        hint.pull(HintPart.TOKEN);
        when(history.pull()).thenReturn("");

        // When
        hint.rollback();
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertEquals("Xyz", actualText);
    }

    private Hint createInstance()
    {
        return new Hint(tokenizer, history);
    }
}
