/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import org.junit.Assert;
import org.junit.Test;

public class SettingsTrackerTest
{
    private static final String Settings = "Settings"; //$NON-NLS-1$
    private static final String NewSettings = "New Settings"; //$NON-NLS-1$

    @SuppressWarnings("nls")
    @Test
    public void shouldRegisterNewSettings()
    {
        // Given
        var tracker = createInstance();

        // When
        var actualRegistered = tracker.register("abc", Settings);

        // Then
        Assert.assertTrue(actualRegistered);
        Assert.assertEquals(1, tracker.size());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldRegisterOtherNewSettings()
    {
        // Given
        var tracker = createInstance();
        tracker.register("abc", Settings);

        // When
        var actualRegistered = tracker.register("abc", NewSettings);

        // Then
        Assert.assertTrue(actualRegistered);
        Assert.assertEquals(1, tracker.size());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldRegisterTheSameSettingsForNewOnwer()
    {
        // Given
        var tracker = createInstance();
        tracker.register("abc", Settings);

        // When
        var actualRegistered = tracker.register("xyz", Settings);

        // Then
        Assert.assertTrue(actualRegistered);
        Assert.assertEquals(2, tracker.size());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotRegisterTheSameSettings()
    {
        // Given
        var tracker = createInstance();
        tracker.register("abc", Settings);

        // When
        var actualRegistered = tracker.register("abc", Settings);

        // Then
        Assert.assertFalse(actualRegistered);
        Assert.assertEquals(1, tracker.size());
    }

    private SettingsTracker createInstance()
    {
        return new SettingsTracker();
    }
}
