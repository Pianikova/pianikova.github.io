/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.e1c.edt.ai.AIContextFactory;
import org.e1c.edt.ai.AIContextParts;
import org.e1c.edt.ai.CodeCompletionType;
import org.e1c.edt.ai.IAIContextSettings;
import org.e1c.edt.ai.IAIContextSplitter;
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
    private final IAIContextSplitter splitter = mock(IAIContextSplitter.class);
    private final IStringNormalizer stringNormalizer = mock(IStringNormalizer.class);
    private final IAIContextSettings contextSettings = mock(IAIContextSettings.class);

    @Parameter(0)
    public String source;

    @Parameter(1)
    public int sourceOffset;

    @Parameter(2)
    public String prefix;

    @Parameter(3)
    public String middle;

    @Parameter(4)
    public String sufix;

    @Parameter(5)
    public int offset;

    @Parameter(6)
    public CodeCompletionType type;

    @Parameter(7)
    public int expectedOffset;

    @Parameter(8)
    public boolean templeted;

    @Parameter(9)
    public boolean success;

    @Parameter(10)
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

        var text = prefix + middle + sufix;
        var parts = new AIContextParts(new Range(0, prefix.length()),
            new Range(prefix.length(), middle.length()), new Range(prefix.length() + middle.length(), sufix.length()));
        when(splitter.split(text, expectedOffset, 99)).thenReturn(parts);
        when(contextSettings.getMaxLength()).thenReturn(99);
        when(contextSettings.isTempleted()).thenReturn(templeted);
        var factory = createInstance();

        // When
        var actualContext = factory.create(source, sourceOffset, text, offset, type);

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
        return new AIContextFactory(splitter, contextSettings, stringNormalizer);
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "", 0, "", "", "", 0, CodeCompletionType.CodeLines, 0, false, true, "" },
                { "", 0, "Abc", "Xyz", "Qwe", 33, CodeCompletionType.CodeLines, 9, false, true, "AbcQwe" },

                { "", 0, "Abc", "Xyz", "Qwe", 1, CodeCompletionType.CodeLines, 1, false, true, "AbcQwe" },
                { "", 0, "Ab" + System.lineSeparator() + "c", "Xyz", "Qwe" + System.lineSeparator(), 1, CodeCompletionType.CodeLines, 1, false, true, "Ab\ncQwe\n" },

                { "", 0, "Abc", "Xyz", "Qwe", 1, CodeCompletionType.CodeLines, 1, true, true, "<PRE> Abc <SUF>Xyz <MID>Qwe" },
                { "", 0, "Abc" + System.lineSeparator(), System.lineSeparator() + "Xyz", "Qwe" + System.lineSeparator(), 1, CodeCompletionType.CodeLines, 1, true, true, "<PRE> Abc\n <SUF>\nXyz <MID>Qwe\n" },
            });
     // @formatter:on
    }
}
