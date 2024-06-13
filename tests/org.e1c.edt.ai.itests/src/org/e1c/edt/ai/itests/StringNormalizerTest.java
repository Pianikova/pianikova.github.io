/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.StringNormalizer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StringNormalizerTest
{
    @Parameter(0)
    public String text;

    @Parameter(1)
    public Boolean cleanLines;

    @Parameter(2)
    public String expectedText;

    @Test
    @Parameters()
    public void shouldNormalize()
    {
        // Given
        var normalizer = createInstance();

        // When
        var actualText = normalizer.normalize(text, cleanLines);

        // Then
        Assert.assertEquals(escape(expectedText), escape(actualText));
    }

    private StringNormalizer createInstance()
    {
        return new StringNormalizer();
    }

    @SuppressWarnings("nls")
    private String escape(String text)
    {
        return text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "", false, "" },
                { " ", false, " " },
                { "\t\t ", false, "\t\t " },
                { "Abc\r\n", false, "Abc\n" },
                { "\r\nAbc\r\n\r\n", false, "\nAbc\n\n" },
                { "\r\n Abc\r\n\r\n\tXyz", false, "\n Abc\n\n\tXyz" },

                { "", true, "" },
                { " ", true, " " },
                { "\t\t ", true, "\t\t " },
                { "\r\nAbc", true, "\nAbc" },
                { "\r\nAbc \t\r\n", true, "\nAbc \t\n" },
                { "\r\n\t\t Abc", true, "\n\t\t Abc" },
                { "\r\n\t\t \r\n   Abc", true, "\t\t \n   Abc" },
                { "\r\n\r\nAbc", true, "\nAbc" },
                { "Abc\r\nXyz", true, "Abc\nXyz" },
                { "Abc\r\n\r\nXyz", true, "Abc\n\nXyz" },
                { "\r\n Abc\r\n\r\n\tXyz", true, "\n Abc\n\n\tXyz" },
                { "\r\n Abc\r\n\r\n\tXyz \n\t  ", true, "\n Abc\n\n\tXyz \n\t  " },
                { "\r\n Abc\r\n  \t\r\n\t Xyz", true, "\n Abc\n  \t\n\t Xyz" },
                { "\r\n Abc\r\n\t\t\t\t\r\n  \t\r\n\t Xyz", true, "\n Abc\n  \t\n\t Xyz" },
            });
     // @formatter:on
    }
}
