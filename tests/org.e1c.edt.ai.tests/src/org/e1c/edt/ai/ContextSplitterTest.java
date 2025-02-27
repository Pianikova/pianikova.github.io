/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ContextSplitterTest
{
    private final IContextSettings contextSettings = mock(IContextSettings.class);

    @Parameter(0)
    public String text;

    @Parameter(1)
    public int offset;

    @Parameter(2)
    public int prefixLength;

    @Parameter(3)
    public int suffixLength;

    @Parameter(4)
    public String prefix;

    @Parameter(5)
    public String sufix;

    @Test
    @Parameters()
    public void shouldSplit()
    {
        // Given
        when(contextSettings.getPrefixLength()).thenReturn(prefixLength);
        when(contextSettings.getSuffixLength()).thenReturn(suffixLength);

        var splitter = new ContextSplitter(contextSettings);

        // When
        var parts = splitter.split(text, offset);
        var actualPrefix = parts.getPrefix().apply(text);
        var actualSufix = parts.getSufix().apply(text);

        // Then
        Assert.assertEquals(prefix, actualPrefix);
        Assert.assertEquals(sufix, actualSufix);
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0} with offsert {2} nad max len {3}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "0123456789", 5, 2, 10, "34", "56789" },
                { "0123456789", 10, 2, 10, "89", "" },
                { "0123456789", 0, 2, 2, "", "01" },
                { "0123456789", 2, 10, 10, "01", "23456789" },
                { "0123456789", 0, 10, 10, "", "0123456789" },
                { "0123456789", 10, 10, 10, "0123456789", "" },
                { "0123456789", 2, 10, 5, "01", "23456" },
                { "", 0, 10, 10, "", "" },
                { "0123456789", 2, 1, 1, "1", "2" },
                { "0123456789", 2, 0, 0, "", "" },
                { "0123456789", 2, 0, 1, "", "2" },
                { "0123456789", 2, 1, 0, "1", "" },
            });
        // @formatter:on
    }
}
