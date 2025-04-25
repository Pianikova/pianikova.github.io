/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;

public class HintTest
{
    private final ICodeCompletionTokenizer tokenizer = mock(ICodeCompletionTokenizer.class);
    private final IHintHistory history = mock(IHintHistory.class);
    private final ISource source1 = mock(ISource.class);
    private final ISource source2 = mock(ISource.class);

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
        hint.initiaize(history, 5, false);

        // When
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz\n", source1));
        hint.append(new Text("Asd", source1));
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertFalse(hint.isEmpty());
        Assert.assertFalse(hint.isBlank());
        Assert.assertEquals(new Text("AbcXyz\nAsd", source1), actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAppendTextWhenDifferentSources()
    {
        // Given
        var hint = createInstance();
        hint.initiaize(history, 5, false);

        // When
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz\n", source1));
        hint.append(new Text("Asd", source2));
        var actualText1 = hint.pull(HintPart.TEXT);
        var actualText2 = hint.pull(HintPart.TEXT);

        // Then
        Assert.assertEquals(new Text("AbcXyz\n", source1), actualText1);
        Assert.assertEquals(new Text("Asd", source2), actualText2);
        Assert.assertTrue(hint.isEmpty());
        Assert.assertTrue(hint.isBlank());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAppendSingleLineWhenSingleWordMode()
    {
        // Given
        var hint = createInstance();
        hint.initiaize(history, 5, true);

        // When
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz\n", source1));
        hint.append(new Text("Asd", source1));
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertFalse(hint.isEmpty());
        Assert.assertFalse(hint.isBlank());
        Assert.assertEquals(new Text("AbcXyz", source1), actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotBeEmptyWhenHasText()
    {
        // Given
        var hint = createInstance();

        // When
        hint.append(new Text("Abc", source1));

        // Then
        Assert.assertFalse(hint.isEmpty());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldBeEmptyWhenHasNoText()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));

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
        hint.append(new Text("Abc", source1));

        // Then
        Assert.assertFalse(hint.isBlank());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotBeEmptyWhenDifferentSources()
    {
        // Given
        var hint = createInstance();
        hint.initiaize(history, 5, false);

        // When
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz\n", source1));
        hint.append(new Text("Asd", source2));

        // Then
        Assert.assertFalse(hint.isEmpty());
        Assert.assertFalse(hint.isBlank());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldBeBlankWhenHasNoText()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz", source2));

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
        hint.append(new Text("  \r   \n  \t\t\r", source1));

        // Then
        Assert.assertTrue(hint.isBlank());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldBeBlankWhenHasBlankTextAndDifferentSources()
    {
        // Given
        var hint = createInstance();

        // When
        hint.append(new Text("  \r   \n  \t\t\r", source1));
        hint.append(new Text("  \t\r", source2));

        // Then
        Assert.assertTrue(hint.isBlank());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldCheckWhenStartWith()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));

        // When
        var actualResult = hint.startsWith('A');

        // Then
        Assert.assertTrue(actualResult);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldCheckWhenStartWithWhenDifferentSources()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz", source2));

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
        hint.append(new Text("Abc", source1));

        // When
        var actualResult = hint.startsWith('B');

        // Then
        Assert.assertFalse(actualResult);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldCheckWhenDoesNotStartWithAndDifferentSources()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        hint.append(new Text("B", source2));

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
        hint.append(new Text("Abc", source1));

        // When
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertEquals(new Text("Abc", source1), actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetTextWhenDifferentSouurces()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz", source1));

        // When
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertEquals(new Text("AbcXyz", source1), actualText);
    }

    @Test
    public void shouldGetTextWhenEmpty()
    {
        // Given
        var hint = createInstance();

        // When
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertEquals(Text.EMPTY, actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullText()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));

        // When
        var actualText = hint.pull(HintPart.TEXT);

        // Then
        Assert.assertEquals(new Text("Abc", source1), actualText);
        verify(history).push(new Text("Abc", source1));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullTextWhenDifferentSources()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz", source2));

        // When
        var actualText1 = hint.pull(HintPart.TEXT);
        var actualText2 = hint.pull(HintPart.TEXT);

        // Then
        Assert.assertEquals(new Text("Abc", source1), actualText1);
        verify(history).push(new Text("Abc", source1));

        Assert.assertEquals(new Text("Xyz", source2), actualText2);
        verify(history).push(new Text("Xyz", source2));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetLine()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        when(tokenizer.getNext(2, "Abc", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Ab", "c"));

        // When
        var actualText = hint.getText(HintPart.LINE);

        // Then
        Assert.assertEquals(new Text("Ab", source1), actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetLineWhenDifferentSources()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz", source2));
        when(tokenizer.getNext(2, "Abc", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Abc", ""));

        // When
        var actualText = hint.getText(HintPart.LINE);

        // Then
        Assert.assertEquals(new Text("Abc", source1), actualText);
    }

    @Test
    public void shouldGetLineWhenEmpty()
    {
        // Given
        var hint = createInstance();

        // When
        var actualText = hint.getText(HintPart.LINE);

        // Then
        Assert.assertEquals(Text.EMPTY, actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullLine()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        when(tokenizer.getNext(2, "Abc", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Ab", "c"));

        // When
        var actualText = hint.pull(HintPart.LINE);

        // Then
        Assert.assertEquals(new Text("Ab", source1), actualText);
        verify(history).push(new Text("Ab", source1));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullLineWhenDiferentSources()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz", source2));
        when(tokenizer.getNext(2, "Abc", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Ab", "c"));
        when(tokenizer.getNext(2, "c", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("c", ""));
        when(tokenizer.getNext(2, "Xyz", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Xyz", ""));

        // When
        var actualText1 = hint.pull(HintPart.LINE);
        var actualText2 = hint.pull(HintPart.LINE);
        var actualText3 = hint.pull(HintPart.LINE);

        // Then
        Assert.assertEquals(new Text("Ab", source1), actualText1);
        verify(history).push(new Text("Ab", source1));

        Assert.assertEquals(new Text("c", source1), actualText2);
        verify(history).push(new Text("c", source1));

        Assert.assertEquals(new Text("Xyz", source2), actualText3);
        verify(history).push(new Text("Xyz", source2));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetToken()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        when(tokenizer.getNext(1, "Abc", Hint.TOKEN_DELIMITER)).thenReturn(new CodeCompletionToken("Ab", "c"));

        // When
        var actualText = hint.getText(HintPart.TOKEN);

        // Then
        Assert.assertEquals(new Text("Ab", source1), actualText);
    }

    @Test
    public void shouldGetTokenWhenEmpty()
    {
        // Given
        var hint = createInstance();

        // When
        var actualText = hint.getText(HintPart.TOKEN);

        // Then
        Assert.assertEquals(Text.EMPTY, actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullToken()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        when(tokenizer.getNext(1, "Abc", Hint.TOKEN_DELIMITER)).thenReturn(new CodeCompletionToken("Ab", "c"));

        // When
        var actualText = hint.pull(HintPart.TOKEN);

        // Then
        Assert.assertEquals(new Text("Ab", source1), actualText);
        verify(history).push(new Text("Ab", source1));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullTokenWhenDifferentSources()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz", source2));
        when(tokenizer.getNext(1, "Abc", Hint.TOKEN_DELIMITER)).thenReturn(new CodeCompletionToken("Ab", "c"));
        when(tokenizer.getNext(1, "c", Hint.TOKEN_DELIMITER)).thenReturn(new CodeCompletionToken("c", ""));
        when(tokenizer.getNext(1, "Xyz", Hint.TOKEN_DELIMITER)).thenReturn(new CodeCompletionToken("Xyz", ""));

        // When
        var actualText1 = hint.pull(HintPart.TOKEN);
        var actualText2 = hint.pull(HintPart.TOKEN);
        var actualText3 = hint.pull(HintPart.TOKEN);

        // Then
        Assert.assertEquals(new Text("Ab", source1), actualText1);
        verify(history).push(new Text("Ab", source1));

        Assert.assertEquals(new Text("c", source1), actualText2);
        verify(history).push(new Text("c", source1));

        Assert.assertEquals(new Text("Xyz", source2), actualText3);
        verify(history).push(new Text("Xyz", source2));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetLines()
    {
        // Given
        var hint = createInstance();
        hint.initiaize(history, 5, false);
        hint.append(new Text("Abc|Xyz|Asd", source1));
        when(tokenizer.getNext(1, "Abc|Xyz|Asd", Hint.LINE_DELIMITER))
            .thenReturn(new CodeCompletionToken("Abc|", "Xyz|Asd"));
        when(tokenizer.getNext(1, "Xyz|Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Xyz|", "Asd"));
        when(tokenizer.getNext(1, "Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Asd", ""));

        // When
        var actualText = hint.getText(HintPart.LINES);

        // Then
        Assert.assertEquals(new Text("Abc|Xyz|Asd", source1), actualText);
    }

    @Test
    public void shouldGetLinesWhenEmpty()
    {
        // Given
        var hint = createInstance();
        hint.initiaize(history, 5, false);

        // When
        var actualText = hint.getText(HintPart.LINES);

        // Then
        Assert.assertEquals(Text.EMPTY, actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullLines()
    {
        // Given
        var hint = createInstance();
        hint.initiaize(history, 5, false);
        hint.append(new Text("Abc|Xyz|Asd", source1));
        when(tokenizer.getNext(1, "Abc|Xyz|Asd", Hint.LINE_DELIMITER))
            .thenReturn(new CodeCompletionToken("Abc|", "Xyz|Asd"));
        when(tokenizer.getNext(1, "Xyz|Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Xyz|", "Asd"));
        when(tokenizer.getNext(1, "Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Asd", ""));

        // When
        var actualText = hint.pull(HintPart.LINES);

        // Then
        Assert.assertEquals(new Text("Abc|Xyz|Asd", source1), actualText);
        verify(history).push(new Text("Abc|Xyz|Asd", source1));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullLinesWhenDifferentSources()
    {
        // Given
        var hint = createInstance();
        hint.initiaize(history, 5, false);
        hint.append(new Text("Abc|Xyz|Asd", source1));
        hint.append(new Text("Que|rty", source2));
        when(tokenizer.getNext(1, "Abc|Xyz|Asd", Hint.LINE_DELIMITER))
            .thenReturn(new CodeCompletionToken("Abc|", "Xyz|Asd"));
        when(tokenizer.getNext(1, "Xyz|Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Xyz|", "Asd"));
        when(tokenizer.getNext(1, "Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Asd", ""));
        when(tokenizer.getNext(1, "Que|rty", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Que|", "rty"));
        when(tokenizer.getNext(1, "rty", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("rty", ""));

        // When
        var actualText1 = hint.pull(HintPart.LINES);
        var actualText2 = hint.pull(HintPart.LINES);

        // Then
        Assert.assertEquals(new Text("Abc|Xyz|Asd", source1), actualText1);
        verify(history).push(new Text("Abc|Xyz|Asd", source1));

        Assert.assertEquals(new Text("Que|rty", source2), actualText2);
        verify(history).push(new Text("Que|rty", source2));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetLinesWhenLimitedByMaxLines()
    {
        // Given
        var hint = createInstance();
        hint.initiaize(history, 2, false);
        hint.append(new Text("Abc|Xyz|Asd", source1));
        when(tokenizer.getNext(1, "Abc|Xyz|Asd", Hint.LINE_DELIMITER))
            .thenReturn(new CodeCompletionToken("Abc|", "Xyz|Asd"));
        when(tokenizer.getNext(1, "Xyz|Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Xyz|", "Asd"));

        // When
        var actualText = hint.getText(HintPart.LINES);

        // Then
        Assert.assertEquals(new Text("Abc|Xyz|", source1), actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetLinesWhenLimitedByMaxLinesAndDifferentSources()
    {
        // Given
        var hint = createInstance();
        hint.initiaize(history, 4, false);
        hint.append(new Text("Abc|Xyz|Asd", source1));
        hint.append(new Text("Que|rty", source2));
        when(tokenizer.getNext(1, "Abc|Xyz|Asd", Hint.LINE_DELIMITER))
            .thenReturn(new CodeCompletionToken("Abc|", "Xyz|Asd"));
        when(tokenizer.getNext(1, "Xyz|Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Xyz|", "Asd"));
        when(tokenizer.getNext(1, "Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Asd", ""));
        when(tokenizer.getNext(1, "Que|rty", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Que|", "rty"));
        when(tokenizer.getNext(1, "rty", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("rty", ""));

        // When
        var actualText1 = hint.pull(HintPart.LINES);
        var actualText2 = hint.pull(HintPart.LINES);

        // Then
        Assert.assertEquals(new Text("Abc|Xyz|Asd", source1), actualText1);
        verify(history).push(new Text("Abc|Xyz|Asd", source1));

        Assert.assertEquals(new Text("Que|rty", source2), actualText2);
        verify(history).push(new Text("Que|rty", source2));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldGetLinesWhenLimitedByMaxLinesWasNotReachdAndDifferentSources()
    {
        // Given
        var hint = createInstance();
        hint.initiaize(history, 2, false);
        hint.append(new Text("Abc|Xyz|Asd", source1));
        hint.append(new Text("Que|rty", source2));
        when(tokenizer.getNext(1, "Abc|Xyz|Asd", Hint.LINE_DELIMITER))
            .thenReturn(new CodeCompletionToken("Abc|", "Xyz|Asd"));
        when(tokenizer.getNext(1, "Xyz|Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Xyz|", "Asd"));
        when(tokenizer.getNext(1, "Asd", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Asd", ""));
        when(tokenizer.getNext(1, "Que|rty", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("Que|", "rty"));
        when(tokenizer.getNext(1, "rty", Hint.LINE_DELIMITER)).thenReturn(new CodeCompletionToken("rty", ""));

        // When
        var actualText1 = hint.pull(HintPart.LINES);
        var actualText2 = hint.pull(HintPart.LINES);
        var actualText3 = hint.pull(HintPart.LINES);

        // Then
        Assert.assertEquals(new Text("Abc|Xyz|", source1), actualText1);
        verify(history).push(new Text("Abc|Xyz|", source1));

        Assert.assertEquals(new Text("Asd", source1), actualText2);
        verify(history).push(new Text("Asd", source1));

        Assert.assertEquals(new Text("Que|rty", source2), actualText3);
        verify(history).push(new Text("Que|rty", source2));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullCharWhenMatch()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));

        // When
        var actualText = hint.pullChar('A');

        // Then
        Assert.assertEquals(new Text("A", source1), actualText);
        verify(history).push(new Text("A", source1));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPullCharWhenMatchWhenDifferentSources()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("A", source1));
        hint.append(new Text("xyz", source2));

        // When
        var actualText1 = hint.pullChar('A');
        var actualText2 = hint.pullChar('x');

        // Then
        Assert.assertEquals(new Text("A", source1), actualText1);
        verify(history).push(new Text("A", source1));

        Assert.assertEquals(new Text("x", source2), actualText2);
        verify(history).push(new Text("x", source2));
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotPullCharWhenNotMatch()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));

        // When
        var actualText = hint.pullChar('b');

        // Then
        Assert.assertEquals(new Text("", source1), actualText);
        verify(history, times(0)).push(any());
    }

    @Test
    public void shouldNotPullCharWhenEmpty()
    {
        // Given
        var hint = createInstance();

        // When
        var actualText = hint.pullChar('A');

        // Then
        Assert.assertEquals(Text.EMPTY, actualText);
        verify(history, times(0)).push(any());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldClear()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        hint.append(new Text("Xyz", source2));

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
        hint.append(new Text("Abc", source1));
        hint.append(new Text(" Xyz", source1));
        when(tokenizer.getNext(1, "Abc Xyz", Hint.TOKEN_DELIMITER)).thenReturn(new CodeCompletionToken("Abc ", "Xyz"));
        hint.pull(HintPart.TOKEN);
        when(history.pull()).thenReturn(new Text("Asd ", source1));

        // When
        hint.rollback();
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertEquals(new Text("Asd Xyz", source1), actualText);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotRollbackWhenHistoryIsEmpty()
    {
        // Given
        var hint = createInstance();
        hint.append(new Text("Abc", source1));
        hint.append(new Text(" Xyz", source1));
        when(tokenizer.getNext(1, "Abc Xyz", Hint.TOKEN_DELIMITER)).thenReturn(new CodeCompletionToken("Abc ", "Xyz"));
        hint.pull(HintPart.TOKEN);
        when(history.pull()).thenReturn(new Text("", source1));

        // When
        hint.rollback();
        var actualText = hint.getText(HintPart.TEXT);

        // Then
        Assert.assertEquals(new Text("Xyz", source1), actualText);
    }

    private Hint createInstance()
    {
        return new Hint(tokenizer, history);
    }
}
