/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.assistent.ResponseLineProcessor;
import org.e1c.edt.ai.assistent.model.AIResponse;
import org.junit.Assert;
import org.junit.Test;

public class ResponseLineProcessorTest
{
    private final IJson json = mock(IJson.class);
    @SuppressWarnings("unchecked")
    private final IObserver<String> observer = mock(IObserver.class);

    @Test
    public void shouldProcessNullLine()
    {
        // Given
        var provcessor = createInstance(1);

        // When
        var actualResult = provcessor.process(observer, null);

        // Then
        Assert.assertTrue(actualResult);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessEmptyLine()
    {
        // Given
        var provcessor = createInstance(1);

        // When
        var actualResult = provcessor.process(observer, "");

        // Then
        Assert.assertTrue(actualResult);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessWhenInvalidPrefix()
    {
        // Given
        var provcessor = createInstance(1);

        // When
        var actualResult = provcessor.process(observer, "Abc");

        // Then
        Assert.assertTrue(actualResult);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcess()
    {
        // Given
        var provcessor = createInstance(1);
        var response = new AIResponse();
        var token = new AIResponse.Token();
        token.setText("Xyz");
        response.setToken(token);

        // When
        when(json.deserialize("Abc", AIResponse.class)).thenReturn(Optional.of(response));
        var actualResult = provcessor.process(observer, ResponseLineProcessor.DATA_LINE_PREFIX + "Abc");

        // Then
        Assert.assertTrue(actualResult);
        verify(observer).onNext("Xyz");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotProcessWhenTokenTextIsEmpty()
    {
        // Given
        var provcessor = createInstance(1);
        var response = new AIResponse();
        var token = new AIResponse.Token();
        token.setText("");
        response.setToken(token);

        // When
        when(json.deserialize("Abc", AIResponse.class)).thenReturn(Optional.of(response));
        var actualResult = provcessor.process(observer, ResponseLineProcessor.DATA_LINE_PREFIX + "Abc");

        // Then
        Assert.assertTrue(actualResult);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotProcessWhenTokenTextIsNull()
    {
        // Given
        var provcessor = createInstance(1);
        var response = new AIResponse();
        var token = new AIResponse.Token();
        token.setText(null);
        response.setToken(token);

        // When
        when(json.deserialize("Abc", AIResponse.class)).thenReturn(Optional.of(response));
        var actualResult = provcessor.process(observer, ResponseLineProcessor.DATA_LINE_PREFIX + "Abc");

        // Then
        Assert.assertTrue(actualResult);
    }

    private ResponseLineProcessor createInstance(int codeCompletionLinesCount)
    {
        return new ResponseLineProcessor(json);
    }
}
