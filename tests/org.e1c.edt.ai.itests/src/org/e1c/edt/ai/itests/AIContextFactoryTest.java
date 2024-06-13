/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.AIContextFactory;
import org.e1c.edt.ai.AIContextParts;
import org.e1c.edt.ai.AIContextSettings;
import org.e1c.edt.ai.IAIContextSplitter;
import org.e1c.edt.ai.IStringNormalizer;
import org.e1c.edt.ai.Range;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.google.inject.Provider;

@RunWith(Parameterized.class)
public class AIContextFactoryTest
{
    private final IAIContextSplitter splitter = mock(IAIContextSplitter.class);
    @SuppressWarnings("unchecked")
    private final Provider<AIContextSettings> contextSettingsProvider = mock(Provider.class);
    private final IStringNormalizer stringNormalizer = mock(IStringNormalizer.class);

    @Parameter(0)
    public String prefix;

    @Parameter(1)
    public String middle;

    @Parameter(2)
    public String sufix;

    @Parameter(3)
    public int offset;

    @Parameter(4)
    public int expectedOffset;

    @Parameter(5)
    public boolean templeted;

    @Parameter(6)
    public boolean success;

    @Parameter(7)
    public String expectedContext;

    @Test
    @Parameters()
    public void shouldCreateContext()
    {
        // Given
        when(stringNormalizer.normalize(any(String.class), any(Boolean.class))).thenAnswer(state -> {
            var text = (String)state.getArguments()[0];
            return text.replace(System.lineSeparator(), "\n"); //$NON-NLS-1$
        });

        when(contextSettingsProvider.get()).thenReturn(new AIContextSettings(99, templeted));
        var text = prefix + middle + sufix;
        var parts = new AIContextParts(new Range(0, prefix.length()),
            new Range(prefix.length(), middle.length()), new Range(prefix.length() + middle.length(), sufix.length()));
        when(splitter.split(text, expectedOffset, 99)).thenReturn(parts);

        var factory = createInstance();

        // When
        var actualContext = factory.create(text, offset);

        // Then
        Assert.assertEquals(success, actualContext.isPresent());
        if (success)
        {
            var ctx = actualContext.get();
            Assert.assertEquals(text, ctx.getText());
            Assert.assertEquals(expectedOffset, ctx.getCursorOffset());
            Assert.assertEquals(expectedContext, ctx.getContext());
        }
    }

    private AIContextFactory createInstance()
    {
        return new AIContextFactory(splitter, contextSettingsProvider, stringNormalizer);
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "", "", "", 0, 0, false, true, "" },
                { "Abc", "Xyz", "Qwe", 33, 9, false, true, "AbcQwe" },

                { "Abc", "Xyz", "Qwe", 1, 1, false, true, "AbcQwe" },
                { "Ab" + System.lineSeparator() + "c", "Xyz", "Qwe" + System.lineSeparator(), 1, 1, false, true, "Ab\ncQwe\n" },

                { "Abc", "Xyz", "Qwe", 1, 1, true, true, "<PRE> Abc <SUF>Xyz <MID>Qwe" },
                { "Abc" + System.lineSeparator(), System.lineSeparator() + "Xyz", "Qwe" + System.lineSeparator(), 1, 1, true, true, "<PRE> Abc\n <SUF>\nXyz <MID>Qwe\n" },
            });
     // @formatter:on
    }
}
