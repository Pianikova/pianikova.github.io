/**
 * Copyright (C) 2024, 1C
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
public class ContextInitializerTest
{
    private final IContextSplitter splitter = mock(IContextSplitter.class);

    @Parameter(0)
    public String prefix;

    @Parameter(1)
    public String sufix;

    @Parameter(2)
    public int textOffset;

    @Parameter(3)
    public int expectedOffset;

    @Parameter(4)
    public boolean success;

    @SuppressWarnings("nls")
    @Test
    @Parameters()
    public void shouldCreateContext()
    {
        // Given
        var text = prefix + sufix;
        var parts = new ContextParts(new Range(0, prefix.length()), new Range(prefix.length(), sufix.length()));
        when(splitter.split(text, expectedOffset)).thenReturn(parts);
        var factory = createInstance();

        // When
        var actualContext =
            factory.initialize(new AIContext(textOffset + 3, "full_" + text, textOffset + 3, "", text, textOffset));

        // Then
        Assert.assertEquals(success, actualContext.isPresent());
        if (success)
        {
            var ctx = actualContext.get();
            Assert.assertEquals("full_" + text, ctx.getSource());
            Assert.assertEquals(textOffset + 3, ctx.getSourceOffset());
            Assert.assertEquals(text, ctx.getText());
            Assert.assertEquals(expectedOffset, ctx.getTextOffset());
            Assert.assertEquals(prefix, ctx.getPrefix());
            Assert.assertEquals(sufix, ctx.getSufix());
        }
    }

    private ContextInitializer createInstance()
    {
        return new ContextInitializer(splitter);
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "", "", 0, 0, true },
                { "Abc", "Xyz", 0, 0, true },
                { "Abc", "Xyz", 3, 3, true },
                { "Abc", "Xyz", 7, 6, true },
            });
     // @formatter:on
    }
}
