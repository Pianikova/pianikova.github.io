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
        var tokenizer = new CodeCompletionTokenizer();

        // When
        var actualToken = tokenizer.getNext(minSize, text, this::isDelimiter);

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
                { "  Hello  Abc", 2, new CodeCompletionToken("  ", "Hello  Abc") },
                { "Hello  Abc", 2, new CodeCompletionToken("Hello  ", "Abc") },
                { "Abc", 2, new CodeCompletionToken("Abc", "") },
                { "  Hello  Abc", 3, new CodeCompletionToken("  Hello  ", "Abc") },
                { "Hello  Abc ", 3, new CodeCompletionToken("Hello  ", "Abc ") },
                { "Abc ", 3, new CodeCompletionToken("Abc ", "") },
                { "Abc", 2, new CodeCompletionToken("Abc", "") },
                { "Abc", 5, new CodeCompletionToken("Abc", "") },
                { "   ", 2, new CodeCompletionToken("   ", "") },
                { "   ", 5, new CodeCompletionToken("   ", "") },
                { "", 5, new CodeCompletionToken("", "") },
            });
        // @formatter:on
    }

    private Boolean isDelimiter(char ch)
    {
        return ch == ' ';
    }
}
