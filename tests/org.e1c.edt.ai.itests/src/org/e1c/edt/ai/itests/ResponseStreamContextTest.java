/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.assistent.ILinesCounter;
import org.e1c.edt.ai.assistent.ResponseStreamContext;
import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.client.AISettings;
import org.junit.Assert;
import org.junit.Test;

public class ResponseStreamContextTest
{
    private final ISettingsProvider settingsProvider = mock(ISettingsProvider.class);
    private final ILinesCounter linesCounter = mock(ILinesCounter.class);

    @SuppressWarnings("nls")
    @Test
    public void shouldAcceptWhen1LineFor1Line()
    {
        // Given
        var context = createInstance(1);

        // When
        when(linesCounter.acceptAndGetLinesCount('A')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('b')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('c')).thenReturn(1);
        var actualLength = context.acceptAndGetLength("Abc");

        // Then
        Assert.assertEquals(3, actualLength);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAcceptWhen1LineFor2Lines()
    {
        // Given
        var context = createInstance(1);

        // When
        when(linesCounter.acceptAndGetLinesCount('A')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('\n')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('c')).thenReturn(2);
        var actualLength = context.acceptAndGetLength("A\nc");

        // Then
        Assert.assertEquals(2, actualLength);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAcceptWhen2LineFor2Lines()
    {
        // Given
        var context = createInstance(2);

        // When
        when(linesCounter.acceptAndGetLinesCount('A')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('\n')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('c')).thenReturn(2);
        var actualLength = context.acceptAndGetLength("A\nc");

        // Then
        Assert.assertEquals(3, actualLength);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAcceptWhen2LineFor3Lines()
    {
        // Given
        var context = createInstance(2);

        // When
        when(linesCounter.acceptAndGetLinesCount('A')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('b')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('\n')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('c')).thenReturn(2);
        when(linesCounter.acceptAndGetLinesCount('\r')).thenReturn(2);
        when(linesCounter.acceptAndGetLinesCount('d')).thenReturn(3);
        when(linesCounter.acceptAndGetLinesCount('e')).thenReturn(3);
        var actualLength = context.acceptAndGetLength("Ab\nc\rde");

        // Then
        Assert.assertEquals(5, actualLength);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAcceptWhen3LineFor3Lines()
    {
        // Given
        var context = createInstance(3);

        // When
        when(linesCounter.acceptAndGetLinesCount('A')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('b')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('\n')).thenReturn(1);
        when(linesCounter.acceptAndGetLinesCount('c')).thenReturn(2);
        when(linesCounter.acceptAndGetLinesCount('\r')).thenReturn(2);
        when(linesCounter.acceptAndGetLinesCount('d')).thenReturn(3);
        when(linesCounter.acceptAndGetLinesCount('e')).thenReturn(3);
        var actualLength = context.acceptAndGetLength("Ab\nc\rde");

        // Then
        Assert.assertEquals(7, actualLength);
    }

    @SuppressWarnings("nls")
    private ResponseStreamContext createInstance(int codeCompletionLinesCount)
    {
        AISettings settings = null;
        try
        {
            settings = new AISettings(List.of(""), List.of(""), new URL("http://abc.com"), new URL("http://abc.com"),
                "", "", "", "", "", new Parameters(), 0, codeCompletionLinesCount);
        }
        catch (MalformedURLException e)
        {
            // TODO Auto-generated catch block
        }

        when(settingsProvider.getSettings()).thenReturn(Optional.ofNullable(settings));
        return new ResponseStreamContext(settingsProvider, linesCounter);
    }
}
