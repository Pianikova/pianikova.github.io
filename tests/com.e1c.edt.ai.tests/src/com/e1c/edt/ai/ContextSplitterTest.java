/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

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

import com.e1c.edt.ai.assistent.model.ProjectId;

@RunWith(Parameterized.class)
public class ContextSplitterTest
{
    private static final ProjectId projectId = ProjectId.Default;
    private final ISettings settings = mock(ISettings.class);

    @Parameter(0)
    public String text;

    @Parameter(1)
    public int offset;

    @Parameter(2)
    public int prefixLength;

    @Parameter(3)
    public int suffixLength;

    @Parameter(4)
    public Boolean limitSize;

    @Parameter(5)
    public String prefix;

    @Parameter(6)
    public String sufix;

    @Test
    @Parameters()
    public void shouldSplit()
    {
        // Given
        when(settings.getPrefixLength(projectId)).thenReturn(prefixLength);
        when(settings.getSuffixLength(projectId)).thenReturn(suffixLength);

        var splitter = new ContextSplitter(settings);

        // When
        var parts = splitter.split(projectId, text, offset, limitSize);
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
                { "0123456789", 5, 2, 10, true, "34", "56789" },
                { "0123456789", 10, 2, 10, true, "89", "" },
                { "0123456789", 0, 2, 2, true, "", "01" },
                { "0123456789", 2, 10, 10, true, "01", "23456789" },
                { "0123456789", 0, 10, 10, true, "", "0123456789" },
                { "0123456789", 10, 10, 10, true, "0123456789", "" },
                { "0123456789", 2, 10, 5, true, "01", "23456" },
                { "", 0, 10, 10, true, "", "" },
                { "0123456789", 2, 1, 1, true, "1", "2" },
                { "0123456789", 2, 0, 0, true, "", "" },
                { "0123456789", 2, 0, 1, true, "", "2" },
                { "0123456789", 2, 1, 0, true, "1", "" },
                { "0123456789", 5, 1, 1, false, "01234", "56789" },
            });
        // @formatter:on
    }
}
