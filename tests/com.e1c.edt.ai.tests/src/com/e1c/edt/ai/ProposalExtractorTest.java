/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ProposalExtractorTest
{
    @Parameter(0)
    public String prefix;

    @Parameter(1)
    public String proposal;

    @Parameter(2)
    public String expectedResult;

    @Test
    public void shouldExtract()
    {
        // Given
        var extractor = new ProposalExtractor();

        // When
        var actualResult = extractor.extract(prefix, proposal).orElse(null);

        // Then
        Assert.assertEquals(expectedResult, actualResult);
    }

    @SuppressWarnings("nls")
    @Parameters
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { "", "", null },
                { "", "  ", null },
                { "", " \t", null },
                { "", "Abc", "Abc" },
                { "", "Abc(", "Abc" },
                { "", "Abc(a, b)", "Abc" },
                { "A", "Abcd", "bcd" },
                { "Ab", "Abcd", "cd" },
                { "Abc", "Abcd", "d" },
                { "Abcd", "Abcd", "" },
                { "AbZ", "Abcd", null },
                { "x", "Abcd", null },
                { "abc", "Abcd", null },
                { " ", "Abcd", "Abcd" },
                { "   ", "Abcd", "Abcd" },
                { " \r\n", "Abcd", "Abcd" },
                { " \n", "Abcd", "Abcd" },
                { " \r\n\t", "Abcd", "Abcd" },
                { " \r\n  ", "Abcd", "Abcd" },
                { ".", "Abcd", "Abcd" },
                { "+", "Abcd", "Abcd" },
            });
        // @formatter:on
    }
}
