/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.AIContextSplitter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AIContextSplitterTest
{
    @Parameter(0)
    public String text;

    @Parameter(1)
    public double midpointFactor;

    @Parameter(2)
    public int offset;

    @Parameter(3)
    public int maxLength;

    @Parameter(4)
    public String prefix;

    @Parameter(5)
    public String middle;

    @Parameter(6)
    public String sufix;


    @Test
    @Parameters()
    public void shouldSplit()
    {
        // Given
        @SuppressWarnings("nls")
        var splitter = new AIContextSplitter("|", midpointFactor);

        // When
        var parts = splitter.split(text.replace('-', '\t'), offset, maxLength);
        var actualPrefix = parts.getPrefix().apply(text);
        var actualSufix = parts.getSufix().apply(text);
        var actualMiddle = parts.getMiddle().apply(text);

        // Then
        Assert.assertEquals(prefix, actualPrefix);
        Assert.assertEquals(sufix, actualSufix);
        Assert.assertEquals(middle, actualMiddle);
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

                { " ", .6666, 0, 1500, "", " ", "" },

                // обесп|// 123|Начало|-Наимен = Коп|-|Конец|
                //   _
                { "// обесп|// 123|Начало|-Наимен = Коп|-|Конец|", .6666, 5, 1500, "", "// об", "есп|// 123|Начало|-Наимен = Коп|-|Конец|" },

                // обесп|// 123|Начало|-Наимен = Коп|-|Конец|
                //           _
                { "// обесп|// 123|Начало|-Наимен = Коп|-|Конец|", .6666, 13, 1500, "// обесп|", "// 1", "23|Начало|-Наимен = Коп|-|Конец|" },

                // обесп|// 123|Начало|-Наимен = Коп|-|Конец|
                //                                    _
                { "// обесп|// 123|Начало|-Наимен = Коп|-|Конец|", .6666, 38, 1500, "// обесп|// 123|Начало|-Наимен = Коп|", "-", "|Конец|" },

                // обесп|// 123|Начало|-Наимен = Коп|--|Конец|
                //                                    _
                { "// обесп|// 123|Начало|-Наимен = Коп|--|Конец|", .6666, 38, 1500, "// обесп|// 123|Начало|-Наимен = Коп|", "-", "-|Конец|" },

                // обесп|// 123|Начало|-Наимен = Коп|-|Конец|
                //             _
                { "// обесп|// 123|Начало|-Наимен = Коп|-|Конец|", .6666, 15, 1500, "// обесп|", "// 123", "|Начало|-Наимен = Коп|-|Конец|" },
            });
        // @formatter:on
    }
}
