/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.CodeCompletionToken;
import org.e1c.edt.ai.CodeCompletionTokenizer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CodeCompletionTokenizerTest
{
    @Parameter(0)
    public String text;

    @Parameter(1)
    public int minSize;

    @Parameter(2)
    public CodeCompletionToken expectedToken;

    @Test
    @Parameters()
    public void shouldTokenize()
    {
        // Given
        var tokenizer = new CodeCompletionTokenizer(minSize);

        // When
        var actualToken = tokenizer.getNext(text);

        // Then
        Assert.assertEquals(expectedToken, actualToken);
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "//Наименование = Наименование + \"!\" + \"?\";", 2, new CodeCompletionToken("//Наименование", " = Наименование + \"!\" + \"?\";") },
                { " = Наименование + \"!\" + \"?\";", 2, new CodeCompletionToken(" = Наименование", " + \"!\" + \"?\";") },
                { " + \"!\" + \"?\";", 2, new CodeCompletionToken(" + \"!\"", " + \"?\";") },
                { " + \"?\";", 2, new CodeCompletionToken("", " + \"?\";") },
                { "Hello", 2, new CodeCompletionToken("", "Hello") },
                { "Hello Abc", 2, new CodeCompletionToken("Hello", " Abc") },
                { "Hello\tAbc", 2, new CodeCompletionToken("Hello", "\tAbc") },
                { "Hello  \t\rAbc", 2, new CodeCompletionToken("Hello", "  \t\rAbc") },
                { "Hello\r\nAbc", 2, new CodeCompletionToken("Hello", "\r\nAbc") },
                { "  Hello  Abc", 2, new CodeCompletionToken("  Hello", "  Abc") },
                { "  Hello  A", 2, new CodeCompletionToken("", "  Hello  A") },
                { "  A  Hello", 2, new CodeCompletionToken("", "  A  Hello") },
                { "", 2, new CodeCompletionToken("", "") },
                { " ", 2, new CodeCompletionToken("", " ") },
                { "  ", 2, new CodeCompletionToken("", "  ") },
            });
        // @formatter:on
    }
}
