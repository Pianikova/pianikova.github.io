/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IParser;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.SettingsProvider;
import org.e1c.edt.ai.assistent.model.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

@SuppressWarnings("nls")
public class SettingsProviderTest
{
    private final ILog log = Mockito.mock(ILog.class);
    private final ISettingsStore settingsStore = Mockito.mock(ISettingsStore.class);
    @SuppressWarnings("unchecked")
    private final IParser<String, Parameters> parametersParser = Mockito.mock(IParser.class);

    @Test
    public void shouldProvideModelName()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.MODELNAME)).thenReturn("Abc");
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals("Abc", settings.getModelName());
    }

    @Test
    public void shouldProvideClientToken()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.CLIENTTOKEN)).thenReturn("Abc");
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals("Abc", settings.getClientToken());
    }

    @Test
    public void shouldProvideDataBaseName()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.DATABASENAME)).thenReturn("Abc");
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals("Abc", settings.getDataBaseName());
    }

    @Test
    public void shouldProvideApiURL()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.CLIENTTOKEN)).thenReturn("Abc");
        when(settingsStore.getString(ISettingsStore.APIURL)).thenReturn("http://api.com");
        var settings = provider.getSettings();

        // Then
        try
        {
            Assert.assertEquals(new URL(new URL("http://api.com"), "generate?client_id=Abc"), settings.getApiURL());
        }
        catch (MalformedURLException e)
        {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void shouldProvideChatURL()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.CHATURL)).thenReturn("http://chat.com");
        var settings = provider.getSettings();

        // Then
        try
        {
            Assert.assertEquals(new URL("http://chat.com"), settings.getChatURL());
        }
        catch (MalformedURLException e)
        {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void shouldProvideTags()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.TAGS)).thenReturn("Tag1,Tag2");
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals(Arrays.asList("Tag1", "Tag2"), settings.getTags());
    }

    @Test
    public void shouldProvideAccessRoles()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.ACCESSROLES)).thenReturn("Role1,Role2");
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals(Arrays.asList("Role1", "Role2"), settings.getAccessRoles());
    }

    @Test
    public void shouldProvideDocumentPath()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.DOCUMENTPATH)).thenReturn("Abc");
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals("Abc", settings.getDocumentPath());
    }

    @Test
    public void shouldProvideLlmParameters()
    {
        // Given
        var parameters = new Parameters();
        var provider = createInstance();

        // When
        when(settingsStore.getString(ISettingsStore.LLMPARAMETERS)).thenReturn("LLM params");
        when(parametersParser.parse("LLM params")).thenReturn(parameters);
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals(parameters, settings.getLlmParameters());
    }

    @Test
    public void shouldProvideMaxAssistantTextSize()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getInt(ISettingsStore.MAXASSISTANTTEXTSIZE)).thenReturn(123);
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals(123, settings.getMaxAssistantTextSize());
    }

    @Test
    public void shouldProvideDefaultMaxAssistantTextSize()
    {
        // Given
        var provider = createInstance();

        // When
        when(settingsStore.getInt(ISettingsStore.MAXASSISTANTTEXTSIZE)).thenReturn(0);
        var settings = provider.getSettings();

        // Then
        Assert.assertEquals(ISettingsStore.DefaultMaxAssistantTextSize, settings.getMaxAssistantTextSize());
    }

    private SettingsProvider createInstance()
    {
        when(settingsStore.getString(Mockito.anyString())).thenReturn("");
        when(settingsStore.getInt(Mockito.anyString())).thenReturn(0);
        when(settingsStore.getString(ISettingsStore.APIURL)).thenReturn("http://api.com");
        when(settingsStore.getString(ISettingsStore.CHATURL)).thenReturn("http://chat.com");
        return new SettingsProvider(log, settingsStore, parametersParser);
    }
}
