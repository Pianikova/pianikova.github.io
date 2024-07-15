/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.ContextFactory;
import org.e1c.edt.ai.ContextParts;
import org.e1c.edt.ai.IContextSettings;
import org.e1c.edt.ai.IContextSplitter;
import org.e1c.edt.ai.IStringNormalizer;
import org.e1c.edt.ai.Range;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AIContextFactoryTest
{
    private final IContextSplitter splitter = mock(IContextSplitter.class);
    private final IStringNormalizer stringNormalizer = mock(IStringNormalizer.class);
    private final IContextSettings contextSettings = mock(IContextSettings.class);

    @Parameter(0)
    public String prefix;

    @Parameter(1)
    public String sufix;

    @Parameter(2)
    public int offset;

    @Parameter(3)
    public int expectedOffset;

    @Parameter(4)
    public boolean success;

    @Test
    @Parameters()
    public void shouldCreateContext()
    {
        // Given
        when(stringNormalizer.normalize(any(String.class), any(Boolean.class))).thenAnswer(state -> {
            var text = (String)state.getArguments()[0];
            return text.replace(System.lineSeparator(), "\n"); //$NON-NLS-1$
        });

        var text = prefix + sufix;
        var parts = new ContextParts(new Range(0, prefix.length()), new Range(prefix.length(), sufix.length()));
        when(splitter.split(text, expectedOffset, 99)).thenReturn(parts);
        when(contextSettings.getMaxLength()).thenReturn(99);
        var factory = createInstance();

        // When
        var actualContext = factory.create("", 0, text, offset); //$NON-NLS-1$

        // Then
        Assert.assertEquals(success, actualContext.isPresent());
        if (success)
        {
            var ctx = actualContext.get();
            Assert.assertEquals(text, ctx.getText());
            Assert.assertEquals(expectedOffset, ctx.getCursorOffset());
            Assert.assertEquals(prefix, ctx.getPrefix());
            Assert.assertEquals(sufix, ctx.getSufix());
        }
    }

    private ContextFactory createInstance()
    {
        return new ContextFactory(splitter, contextSettings, stringNormalizer);
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
