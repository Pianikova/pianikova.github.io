/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.assistent.SettingsTracker;
import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.client.AISettings;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class SettingsTrackerTest
{
    private final AISettings Settings;
    private final AISettings NewURLSettings;
    private final AISettings NewTokenSettings;
    private final AISettings NewLllmParamsSettings;
    private final IJson json = Mockito.mock(IJson.class);

    @SuppressWarnings("nls")
    public SettingsTrackerTest()
    {
        URL apiURL = null;
        URL apiURL2 = null;
        try
        {
            apiURL = new URL("https://llms.1c.ai/code/api/v1/");
            apiURL2 = new URL("https://llms.1c.ai/code_stage/api/v1/");
        }
        catch (MalformedURLException e)
        {
            // ignored
        }

        String clientToken = "abc";
        String clientToken2 = "abc2";
        String clientUniqueId = "Nik";
        Parameters llmParameters = new Parameters();
        Parameters llmParameters2 = new Parameters();
        llmParameters2.maxNewTokens = 123;
        Settings = new AISettings(apiURL, clientToken, clientUniqueId, llmParameters);
        NewURLSettings = new AISettings(apiURL2, clientToken, clientUniqueId, llmParameters);
        NewTokenSettings = new AISettings(apiURL, clientToken2, clientUniqueId, llmParameters);
        NewLllmParamsSettings = new AISettings(apiURL, clientToken, clientUniqueId, llmParameters2);

        when(json.serialize(Settings)).thenReturn("my Settings");
        when(json.serialize(NewURLSettings)).thenReturn("my NewURLSettings");
        when(json.serialize(NewTokenSettings)).thenReturn("my NewTokenSettings");
        when(json.serialize(NewLllmParamsSettings)).thenReturn("my NewLllmParamsSettings");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldRegisterNewSettings()
    {
        // Given
        var tracker = createInstance();

        // When
        var actualRegistered = tracker.register("abc", Optional.of(Settings));

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
        tracker.register("abc", Optional.of(Settings));

        // When
        var actualRegistered = tracker.register("xyz", Optional.of(Settings));

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
        tracker.register("abc", Optional.of(Settings));

        // When
        var actualRegistered = tracker.register("abc", Optional.of(Settings));

        // Then
        Assert.assertFalse(actualRegistered);
        Assert.assertEquals(1, tracker.size());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldRegisterWhenEmpty()
    {
        // Given
        var tracker = createInstance();

        // When
        var actualRegistered = tracker.register("abc", Optional.empty());

        // Then
        Assert.assertTrue(actualRegistered);
        Assert.assertEquals(0, tracker.size());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldRegisterNewSettingsWhenEmpty()
    {
        // Given
        var tracker = createInstance();
        tracker.register("abc", Optional.of(Settings));

        // When
        var actualRegistered = tracker.register("abc", Optional.empty());

        // Then
        Assert.assertTrue(actualRegistered);
        Assert.assertEquals(0, tracker.size());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldRegisterWhenURLChanged()
    {
        // Given
        var tracker = createInstance();
        tracker.register("abc", Optional.of(Settings));

        // When
        var actualRegistered = tracker.register("abc", Optional.of(NewURLSettings));

        // Then
        Assert.assertTrue(actualRegistered);
        Assert.assertEquals(1, tracker.size());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldRegisterWhenUserTokenChanged()
    {
        // Given
        var tracker = createInstance();
        tracker.register("abc", Optional.of(Settings));

        // When
        var actualRegistered = tracker.register("abc", Optional.of(NewTokenSettings));

        // Then
        Assert.assertTrue(actualRegistered);
        Assert.assertEquals(1, tracker.size());
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldRegisterWhenUserLllmParamsChanged()
    {
        // Given
        var tracker = createInstance();
        tracker.register("abc", Optional.of(Settings));

        // When
        var actualRegistered = tracker.register("abc", Optional.of(NewLllmParamsSettings));

        // Then
        Assert.assertTrue(actualRegistered);
        Assert.assertEquals(1, tracker.size());
    }

    private SettingsTracker createInstance()
    {
        return new SettingsTracker(json);
    }
}
