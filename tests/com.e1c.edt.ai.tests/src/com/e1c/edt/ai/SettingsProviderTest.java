/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import static org.mockito.Mockito.when;

import java.util.Optional;

import com.e1c.edt.ai.assistent.model.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

@SuppressWarnings("nls")
public class SettingsProviderTest
{
    private final IDefaultSettings defaultSettings = Mockito.mock(IDefaultSettings.class);
    private final ISettingsStore settingsStore = Mockito.mock(ISettingsStore.class);
    private final IIdProvider idProvider = Mockito.mock(IIdProvider.class);
    @SuppressWarnings("unchecked")
    private final IParser<String, Parameters> parametersParser = Mockito.mock(IParser.class);

    public SettingsProviderTest()
    {
        when(defaultSettings.getUrl()).thenReturn("http://abc.ru");
    }

    @Test
    public void shouldProvideClientToken()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.CLIENT_TOKEN)).thenReturn("Abc");
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals("Abc", settings.getClientToken());
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
        Assert.assertEquals("Abc", settings.getClientToken());
    }

    @Test
    public void shouldProvideLlmParameters()
    {
        // Given
        var parameters = new Parameters(defaultSettings);
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.PARAMETERS)).thenReturn("LLM params");
        when(parametersParser.parse("LLM params")).thenReturn(Optional.of(parameters));
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals(parameters, settings.getLlmParameters());
    }

    private SettingsProvider createInstance()
    {
        when(settingsStore.getString(Mockito.anyString())).thenReturn("");
        when(idProvider.getId()).thenReturn("");
        when(settingsStore.getInt(Mockito.anyString())).thenReturn(0);
        var params = Optional.of(new Parameters(defaultSettings));
        when(parametersParser.parse(Mockito.anyString())).thenReturn(params);
        return new SettingsProvider(settingsStore, parametersParser, idProvider, defaultSettings);
    }
}
