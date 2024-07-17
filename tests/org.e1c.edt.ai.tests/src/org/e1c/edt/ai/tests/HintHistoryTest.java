/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import static org.mockito.Mockito.mock;

import org.e1c.edt.ai.HintHistory;
import org.e1c.edt.ai.ISource;
import org.e1c.edt.ai.Text;
import org.junit.Assert;
import org.junit.Test;

public class HintHistoryTest
{
    private final ISource source = mock(ISource.class);

    @Test
    public void shouldBeEmptyWhenCreated()
    {
        // Given

        // When
        var history = new HintHistory();

        // Then
        Assert.assertTrue(history.isEmpty());
    }

    @Test
    public void shouldPullEmptyStringWhenEmpty()
    {
        // Given
        var history = new HintHistory();

        // When
        var actualStr = history.pull();

        // Then
        Assert.assertEquals(Text.EMPTY, actualStr);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPushAndPull()
    {
        // Given
        var history = new HintHistory();

        // When
        history.push(new Text("Abc", source));
        history.push(new Text("Xyz", source));
        var actualItem1 = history.pull();
        var actualItem2 = history.pull();

        // Then
        Assert.assertEquals(new Text("Xyz", source), actualItem1);
        Assert.assertEquals(new Text("Abc", source), actualItem2);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldBeEmptyWhenAllPulled()
    {
        // Given
        var history = new HintHistory();

        // When
        history.push(new Text("Abc", source));
        history.push(new Text("Xyz", source));
        history.pull();
        history.pull();

        // Then
        Assert.assertTrue(history.isEmpty());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldClear()
    {
        // Given
        var history = new HintHistory();
        history.push(new Text("Abc", source));
        history.push(new Text("Xyz", source));

        // When
        history.clear();
        var actualItem = history.pull();

        // Then
        Assert.assertTrue(history.isEmpty());
        Assert.assertEquals(Text.EMPTY, actualItem);
    }
}
