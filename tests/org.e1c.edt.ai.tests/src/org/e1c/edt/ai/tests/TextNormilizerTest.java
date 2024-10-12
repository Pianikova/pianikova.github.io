/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.TextNormilizer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TextNormilizerTest
{
    @Parameter(0)
    public String text;

    @Parameter(1)
    public String expectedNormalizedText;

    @SuppressWarnings("nls")
    @Test
    public void shouldBuild()
    {
        // Given
        var normilizer = new TextNormilizer();

        // When
        var actualNormalizedText = normilizer.normalize(text);

        // Then
        Assert.assertEquals(expectedNormalizedText.replace("\n", "\\n").replace("\r", "\\r"),
            actualNormalizedText.replace("\n", "\\n").replace("\r", "\\r"));
        Assert.assertEquals(expectedNormalizedText.length(), actualNormalizedText.length());
    }

    @SuppressWarnings("nls")
    @Parameters
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "Abc", "Abc" },
                { "", "" },
                { "Abc\r\nXyz", "Abc\r\nXyz" },
                { "Abc\r\rXyz", "Abc\r\nXyz" },
                { "Abc\r\r\nXyz", "Abc\r\n\nXyz" },
                { "Abc\r\r\rXyz", "Abc\r\n\nXyz" },
                { "Abc\r\r\r\rXyz", "Abc\r\n\r\nXyz" },
                { "\r\r\r\r", "\r\n\r\n" },
                { "Abc\r\n\rXyz", "Abc\r\n\nXyz" },
                { "Abc\r\n\r", "Abc\r\n\n" },
                { "\r\n\rXyz", "\r\n\nXyz" },
                { "\r\n\r", "\r\n\n" },
            });
        // @formatter:on
    }
}
