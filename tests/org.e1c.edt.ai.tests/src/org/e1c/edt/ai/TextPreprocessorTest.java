/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.assistent.TextPreprocessor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TextPreprocessorTest
{
    private final IUISettings uiSettings = mock(IUISettings.class);

    @Parameter(0)
    public String text;

    @Parameter(1)
    public String expectedText;

    @SuppressWarnings("nls")
    @Test
    public void shouldBuild()
    {
        // Given
        when(uiSettings.getLineSeparator()).thenReturn("|");
        var preprocessor = new TextPreprocessor(uiSettings);

        // When
        var actualText = preprocessor.process(text);

        // Then
        Assert.assertEquals(expectedText.replace("\n", "\\n").replace("\r", "\\r"),
            actualText.replace("\n", "\\n").replace("\r", "\\r"));
        Assert.assertEquals(expectedText.length(), actualText.length());
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
                { "Abc\r\nXyz", "Abc|Xyz" },
                { "Abc\r\rXyz", "Abc||Xyz" },
                { "Abc\r\r\nXyz", "Abc||Xyz" },
                { "Abc\r\r\rXyz", "Abc|||Xyz" },
                { "Abc\r\r\r\rXyz", "Abc||||Xyz" },
                { "\r\r\r\r", "||||" },
                { "Abc\r\n\rXyz", "Abc||Xyz" },
                { "Abc\r\n\r", "Abc||" },
                { "\r\n\rXyz", "||Xyz" },
                { "\r\n\r", "||" },
            });
        // @formatter:on
    }
}
