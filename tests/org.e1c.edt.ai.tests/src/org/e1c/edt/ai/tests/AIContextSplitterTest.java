/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.ContextSplitter;
import org.e1c.edt.ai.IContextSettings;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AIContextSplitterTest
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
        var parts = splitter.split(text.replace('-', '\t'), offset);
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
                // | is line separator
                // - is tab
                // _ is cursor

                { " ", 0, 1000, 500, "", " " },
                { " ", 1, 1000, 500, " ", "" },

                // обесп|// 123|Начало|-Наимен = Коп|-|Конец|
                //   _
                { "// обесп|// 123|Начало|-Наимен = Коп|-|Конец|", 5, 1000, 500, "// об", "есп|// 123|Начало|-Наимен = Коп|-|Конец|" },

                // обесп|// 123|Начало|-Наимен = Коп|-|Конец|
                //           _
                { "// обесп|// 123|Начало|-Наимен = Коп|-|Конец|", 13, 1000, 500, "// обесп|// 1", "23|Начало|-Наимен = Коп|-|Конец|" },

                // обесп|// 123|Начало|-Наимен = Коп|-|Конец|
                //                                    _
                { "// обесп|// 123|Начало|-Наимен = Коп|-|Конец|", 38, 1000, 500, "// обесп|// 123|Начало|-Наимен = Коп|-", "|Конец|" },

                // обесп|// 123|Начало|-Наимен = Коп|--|Конец|
                //                                    _
                { "// обесп|// 123|Начало|-Наимен = Коп|--|Конец|", 38, 1000, 500, "// обесп|// 123|Начало|-Наимен = Коп|-", "-|Конец|" },

                // обесп|// 123|Начало|-Наимен = Коп|-|Конец|
                //             _
                { "// обесп|// 123|Начало|-Наимен = Коп|-|Конец|", 15, 1000, 500, "// обесп|// 123", "|Начало|-Наимен = Коп|-|Конец|" },

                // Suffix and prefix redistribution
                { "123456789", 5, 2, 10, "12345", "6789" },
                { "123456789", 5, 2, 9, "12345", "6789" },
                { "123456789", 5, 2, 7, "12345", "6789" },
                { "123456789", 5, 2, 6, "2345", "6789" },
                { "123456789", 5, 2, 5, "345", "6789" },
                { "123456789", 5, 2, 4, "45", "6789" },
                { "123456789", 5, 2, 3, "45", "678" },
                { "123456789", 5, 2, 2, "45", "67" },
                { "123456789", 5, 2, 1, "45", "6" },
                { "123456789", 5, 2, 0, "45", "" },
                { "123456789", 5, 2, -1, "45", "" },
                { "123456789", 5, 2, -99, "45", "" },

                { "123456789", 5, 10, 2, "12345", "6789" },
                { "123456789", 5, 9, 2, "12345", "6789" },
                { "123456789", 5, 8, 2, "12345", "6789" },
                { "123456789", 5, 7, 2, "12345", "6789" },
                { "123456789", 5, 6, 2, "12345", "678" },
                { "123456789", 5, 5, 2, "12345", "67" },
                { "123456789", 5, 4, 2, "2345", "67" },
                { "123456789", 5, 3, 2, "345", "67" },
                { "123456789", 5, 2, 2, "45", "67" },
                { "123456789", 5, 1, 2, "5", "67" },
                { "123456789", 5, 0, 2, "", "67" },
                { "123456789", 5, -1, 2, "", "67" },
                { "123456789", 5, -99, 2, "", "67" },
            });
        // @formatter:on
    }
}
