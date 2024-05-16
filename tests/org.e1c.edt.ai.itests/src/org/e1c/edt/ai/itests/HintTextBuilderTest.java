/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.HintTextBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class HintTextBuilderTest
{
    @Parameter(0)
    public String text;

    @Parameter(1)
    public String prefix;

    @Parameter(2)
    public int tabWidth;

    @Parameter(3)
    public String expectedHint;

    @Test
    @Parameters()
    public void shouldBuild()
    {
        // Given
        var builder = new HintTextBuilder();

        // When
        var actualHint = builder.build(text, prefix, tabWidth, '!');

        // Then
        Assert.assertEquals(expectedHint, actualHint);
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0} {1} {2}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "", "", 2, "!" },
                { "Abc", "", 2, "Abc!" },
                { "Abc\nXyz", "", 2, "Abc\nXyz!" },
                { "PrefixAbc", "Prefix", 2, "PrefixAbc!" },
                { "PrefixAbc\nPrefixXyz", "Prefix", 2, "PrefixAbc\nXyz!" },
                { "Abc\nPrefixXyz", "Prefix", 2, "Abc\nXyz!" },
                { "PrefixAbc\nPrefixXyz\nPrefix Asd", "Prefix", 2, "PrefixAbc\nXyz\n Asd!" },
                { "PrefixAbc\n\t\tXyz", "\t\t", 2, "PrefixAbc\nXyz!" },
                { "PrefixAbc\n\t  Xyz", "\t\t", 2, "PrefixAbc\nXyz!" },
                { "PrefixAbc\n    Xyz", "\t\t", 2, "PrefixAbc\nXyz!" },
                { "PrefixAbc\n\t  Xyz", "  \t", 2, "PrefixAbc\nXyz!" },
                { "PrefixAbc\n\t  Xyz", "\t  ", 2, "PrefixAbc\nXyz!" },
                { "PrefixAbc\n\t   Xyz", "\t  ", 2, "PrefixAbc\n Xyz!" },
            });
        // @formatter:on
    }
}
