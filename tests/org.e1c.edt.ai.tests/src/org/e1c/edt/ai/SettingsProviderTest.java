/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.e1c.edt.ai.assistent.model.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

@SuppressWarnings("nls")
public class SettingsProviderTest
{
    private final ILog log = Mockito.mock(ILog.class);
    private final ISettingsStore settingsStore = Mockito.mock(ISettingsStore.class);
    private final IIdProvider idProvider = Mockito.mock(IIdProvider.class);
    @SuppressWarnings("unchecked")
    private final IParser<String, Parameters> parametersParser = Mockito.mock(IParser.class);

    @Test
    public void shouldProvideClientToken()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.CLIENT_TOKEN)).thenReturn("Abc");
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals("Abc", settings.get().getClientToken());
    }

    @Test
    public void shouldProvideAndTrimClientToken()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.CLIENT_TOKEN)).thenReturn(" \tAbc  \t");
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals("Abc", settings.get().getClientToken());
    }

    @Test
    public void shouldProvideApiURL()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.CLIENT_TOKEN)).thenReturn("Abc");
        when(idProvider.getId()).thenReturn("345");
        when(settingsStore.getString(ISettingsStore.APIURL)).thenReturn("http://api.com/");
        var settings = provider.getSettings();

        // Then
        try
        {
            Assert.assertEquals(new URL("http://api.com/"),
                settings.get().getApiURL());
        }
        catch (MalformedURLException e)
        {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void shouldProvideApiURLWhenHasNoFinishingSlash()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.CLIENT_TOKEN)).thenReturn("Abc");
        when(idProvider.getId()).thenReturn("345");
        when(settingsStore.getString(ISettingsStore.APIURL)).thenReturn("http://api.com");
        var settings = provider.getSettings();

        // Then
        try
        {
            Assert.assertEquals(new URL("http://api.com/"), settings.get().getApiURL());
        }
        catch (MalformedURLException e)
        {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void shouldProvideApiURLWhenHasRelativePath()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.CLIENT_TOKEN)).thenReturn("Abc");
        when(idProvider.getId()).thenReturn("345");
        when(settingsStore.getString(ISettingsStore.APIURL)).thenReturn("http://api.com/generate/");
        var settings = provider.getSettings();

        // Then
        try
        {
            Assert.assertEquals(new URL("http://api.com/generate/"),
                settings.get().getApiURL());
        }
        catch (MalformedURLException e)
        {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void shouldProvideApiURLWhenHasRelativePathHasNoFinishingSlash()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.CLIENT_TOKEN)).thenReturn("Abc");
        when(idProvider.getId()).thenReturn("345");
        when(settingsStore.getString(ISettingsStore.APIURL)).thenReturn("http://api.com/generate");
        var settings = provider.getSettings();

        // Then
        try
        {
            Assert.assertEquals(new URL("http://api.com/generate/"),
                settings.get().getApiURL());
        }
        catch (MalformedURLException e)
        {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void shouldProvideLlmParameters()
    {
        // Given
        var parameters = new Parameters();
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.LLM_PARAMETERS)).thenReturn("LLM params");
        when(parametersParser.parse("LLM params")).thenReturn(Optional.of(parameters));
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals(parameters, settings.get().getLlmParameters());
    }

    @Test
    public void shouldProvideDefaultLlmParametersWhenEppty()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.LLM_PARAMETERS)).thenReturn("LLM params");
        when(parametersParser.parse("LLM params")).thenReturn(Optional.empty());
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals(true, settings.isPresent());
    }

    private SettingsProvider createInstance()
    {
        when(settingsStore.getString(Mockito.anyString())).thenReturn("");
        when(idProvider.getId()).thenReturn("");
        when(settingsStore.getInt(Mockito.anyString())).thenReturn(0);
        when(settingsStore.getString(ISettingsStore.APIURL)).thenReturn("http://api.com");
        when(parametersParser.parse(Mockito.anyString())).thenReturn(Optional.of(new Parameters()));
        return new SettingsProvider(log, settingsStore, parametersParser, idProvider);
    }
}
