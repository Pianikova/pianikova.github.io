/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import org.e1c.edt.ai.HintHistory;
import org.junit.Assert;
import org.junit.Test;

public class HintHistoryTest
{
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
        Assert.assertEquals("", actualStr); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPushAndPull()
    {
        // Given
        var history = new HintHistory();

        // When
        history.push("Abc");
        history.push("Xyz");
        var actualItem1 = history.pull();
        var actualItem2 = history.pull();

        // Then
        Assert.assertEquals("Xyz", actualItem1);
        Assert.assertEquals("Abc", actualItem2);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldBeEmptyWhenAllPulled()
    {
        // Given
        var history = new HintHistory();

        // When
        history.push("Abc");
        history.push("Xyz");
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
        history.push("Abc");
        history.push("Xyz");

        // When
        history.clear();
        var actualItem = history.pull();

        // Then
        Assert.assertTrue(history.isEmpty());
        Assert.assertEquals("", actualItem);
    }
}
