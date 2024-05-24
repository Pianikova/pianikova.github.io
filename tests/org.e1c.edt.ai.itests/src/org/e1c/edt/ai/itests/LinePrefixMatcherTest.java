/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.LinePrefixMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class LinePrefixMatcherTest
{
    @Parameter(0)
    public String line;

    @Parameter(1)
    public String prefix;

    @Parameter(2)
    public int tabWidth;

    @Parameter(3)
    public int expectedPrefixLength;

    @Test
    @Parameters()
    public void shouldMatchPrefix()
    {
        // Given
        var matcher = new LinePrefixMatcher();

        // When
        var actualPrefixLength = matcher.getPrefixLength(line, prefix, tabWidth);

        // Then
        Assert.assertEquals(expectedPrefixLength, actualPrefixLength);
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "\t\tAbc", "\t\t", 2, 2 },
                { "\t  Abc", "\t  ", 2, 3 },
                { "\t\tAbc", "\t  ", 2, 2 },
                { "\t  Abc", "\t\t", 2, 3 },
                { "    Abc", "\t\t", 2, 4 },
                { "\t\tAbc", "    ", 2, 2 },

                { "\tAbc", "\t\t", 2, 0 },
                { "Abc", "\t\t", 2, 0 },
                { "Abc", "\t", 2, 0 },
                { "Abc", "", 2, 0 },
                { "", "\t", 2, 0 },
                { "\tAbc", "", 2, 0 },
            });
        // @formatter:on
    }
}
