/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.AIContextParts;
import org.e1c.edt.ai.AIContextSplitter;
import org.e1c.edt.ai.Range;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AIContextSplitterTests
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
    public AIContextParts expectedParts;

    @Test
    @Parameters()
    public void shouldSplit()
    {
        // Given
        @SuppressWarnings("nls")
        var splitter = new AIContextSplitter("|", midpointFactor);

        // When
        var actualParts = splitter.split(text, offset, maxLength);

        // Then
        Assert.assertEquals(expectedParts, actualParts);
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0} with offsert {2} nad max len {3}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "", .5, 0, 10, new AIContextParts(Range.EMPTY, Range.EMPTY) },
                { "H", .5, 1, 10, new AIContextParts(new Range(0, 1), new Range(1, 0)) },
                { "H", .5, 0, 10, new AIContextParts(new Range(0, 0), new Range(0, 1)) },
                { "HelloWorld", .5, 5, 10, new AIContextParts(new Range(0, 5), new Range(5, 5)) },
                { "Hell|World", .5, 5, 10, new AIContextParts(new Range(0, 5), new Range(5, 5)) },
                { "Helld|orld", .5, 5, 10, new AIContextParts(new Range(0, 5), new Range(5, 5)) },
                { "HelloWorld", .5, 5, 20, new AIContextParts(new Range(0, 5), new Range(5, 5)) },
                { "HelloWorld", .5, 5, 2, new AIContextParts(new Range(4, 1), new Range(5, 1)) },
                { "HelloWorld", .5, 5, 3, new AIContextParts(new Range(3, 2), new Range(5, 1)) },
                { "H|lloWor|d", .5, 5, 10, new AIContextParts(new Range(0, 5), new Range(5, 5)) },
                { "H|lloWor|d", .5, 5, 9, new AIContextParts(new Range(2, 3), new Range(5, 3)) },
                { "H|lloWor|d", .75, 5, 10, new AIContextParts(new Range(0, 5), new Range(5, 5)) },
                { "H|lloWor|d", .75, 5, 9, new AIContextParts(new Range(2, 3), new Range(5, 2)) },
                { "H|lloWorld", .5, 5, 10, new AIContextParts(new Range(0, 5), new Range(5, 5)) },
                { "H|lloWorld", .5, 5, 9, new AIContextParts(new Range(2, 3), new Range(5, 4)) },
                { "|elloWorl|", .5, 5, 10, new AIContextParts(new Range(0, 5), new Range(5, 5)) },
                { "|elloWorl|", .5, 5, 9, new AIContextParts(new Range(1, 4), new Range(5, 4)) },
            });
        // @formatter:on
    }
}
