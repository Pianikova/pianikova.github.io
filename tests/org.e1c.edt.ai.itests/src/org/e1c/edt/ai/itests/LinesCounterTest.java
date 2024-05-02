/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.e1c.edt.ai.assistent.LinesCounter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class LinesCounterTest
{
    @Parameter(0)
    public List<TestCase> testCases;

    @Test
    @Parameters()
    public void shouldCountLines()
    {
        // Given
        var counter = new LinesCounter();

        // When
        for (var testCase : testCases)
        {
            // Then
            Assert.assertEquals(testCase.count, counter.acceptAndGetLinesCount(testCase.ch));
        }
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        return Arrays.asList(
            new Object[][] {
                { List.of(new TestCase('a', 1))},
                { List.of(new TestCase('a', 1), new TestCase('b', 1))},
                { List.of(new TestCase('a', 1), new TestCase('b', 1), new TestCase('\n', 1))},
                { List.of(new TestCase('a', 1), new TestCase('b', 1), new TestCase('\n', 1), new TestCase('x', 2))},
                { List.of(new TestCase('a', 1), new TestCase('b', 1), new TestCase('\n', 1), new TestCase('x', 2), new TestCase('y', 2))},
                { List.of(new TestCase('a', 1), new TestCase('b', 1), new TestCase('\n', 1), new TestCase('x', 2), new TestCase('y', 2), new TestCase('\n', 2))},
                { List.of(new TestCase('a', 1), new TestCase('b', 1), new TestCase('\n', 1), new TestCase('x', 2), new TestCase('y', 2), new TestCase('\n', 2), new TestCase('z', 3))},
                { List.of(new TestCase('a', 1), new TestCase('\n', 1), new TestCase('x', 2))},
                { List.of(new TestCase('\n', 0))},
                { List.of(new TestCase('\r', 0))},
                { List.of(new TestCase('\n', 0), new TestCase('a', 1), new TestCase('b', 1), new TestCase('\n', 1))},
                { List.of(new TestCase('\r', 0), new TestCase('\n', 0), new TestCase('a', 1), new TestCase('b', 1), new TestCase('\n', 1))},
            });
        // @formatter:on
    }

    private static class TestCase
    {
        private char ch;
        private int count;

        public TestCase(char ch, int count)
        {
            this.ch = ch;
            this.count = count;
        }
    }
}
