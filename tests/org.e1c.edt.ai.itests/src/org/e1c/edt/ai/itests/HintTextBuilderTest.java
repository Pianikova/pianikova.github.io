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
    public int tabWidth;

    @Parameter(2)
    public String expectedHint;

    @Test
    @Parameters()
    public void shouldBuild()
    {
        // Given
        var builder = new HintTextBuilder();

        // When
        var actualHint = builder.build(text, tabWidth, '!');

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
                { "", 2, "!" },
                { "Abc", 2, "Abc" },
                { "Abc\nXyz", 2, "Abc\nXyz!" },
                { "\tAbc", 2, "  Abc" },
                { "\t Abc\n\tXyz", 2, "   Abc\n  Xyz!" },
                { "A\t\tbc", 2, "A    bc" },
            });
        // @formatter:on
    }
}
