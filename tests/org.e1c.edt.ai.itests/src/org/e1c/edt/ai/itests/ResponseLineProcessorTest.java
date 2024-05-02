/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.assistent.IResponseStreamContext;
import org.e1c.edt.ai.assistent.ResponseLineProcessor;
import org.e1c.edt.ai.assistent.model.AIResponse;
import org.junit.Assert;
import org.junit.Test;

public class ResponseLineProcessorTest
{
    private final IJson json = mock(IJson.class);
    private final IResponseStreamContext context = mock(IResponseStreamContext.class);
    @SuppressWarnings("unchecked")
    private final IObserver<String> observer = mock(IObserver.class);

    @Test
    public void shouldProcessNullLine()
    {
        // Given
        var provcessor = createInstance(1);

        // When
        var actualResult = provcessor.process(context, observer, null);

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
        var actualResult = provcessor.process(context, observer, "");

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
        var actualResult = provcessor.process(context, observer, "Abc");

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
        when(json.deserialize("Abc", AIResponse.class)).thenReturn(response);
        when(context.acceptAndGetLength("Xyz")).thenReturn(3);
        when(observer.onNext("Xyz")).thenReturn(true);
        var actualResult = provcessor.process(context, observer, ResponseLineProcessor.DATA_LINE_PREFIX + "Abc");

        // Then
        Assert.assertTrue(actualResult);
        verify(observer).onNext("Xyz");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldAbortProcessingWhenOnNextReturnsFalse()
    {
        // Given
        var provcessor = createInstance(1);
        var response = new AIResponse();
        var token = new AIResponse.Token();
        token.setText("Xyz");
        response.setToken(token);

        // When
        when(json.deserialize("Abc", AIResponse.class)).thenReturn(response);
        when(context.acceptAndGetLength("Xyz")).thenReturn(3);
        when(observer.onNext("Xyz")).thenReturn(false);
        var actualResult = provcessor.process(context, observer, ResponseLineProcessor.DATA_LINE_PREFIX + "Abc");

        // Then
        Assert.assertFalse(actualResult);
        verify(observer).onNext("Xyz");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldFinishProcessingWnenCompleted()
    {
        // Given
        var provcessor = createInstance(1);
        var response = new AIResponse();
        var token = new AIResponse.Token();
        token.setText("Xyz");
        response.setToken(token);

        // When
        when(json.deserialize("Abc", AIResponse.class)).thenReturn(response);
        when(context.acceptAndGetLength("Xyz")).thenReturn(2);
        when(observer.onNext("Xy")).thenReturn(true);
        var actualResult = provcessor.process(context, observer, ResponseLineProcessor.DATA_LINE_PREFIX + "Abc");

        // Then
        Assert.assertFalse(actualResult);
        verify(observer).onNext("Xy");
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
        when(json.deserialize("Abc", AIResponse.class)).thenReturn(response);
        var actualResult = provcessor.process(context, observer, ResponseLineProcessor.DATA_LINE_PREFIX + "Abc");

        // Then
        Assert.assertTrue(actualResult);
        verify(context, times(0)).acceptAndGetLength("Xyz");
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
        when(json.deserialize("Abc", AIResponse.class)).thenReturn(response);
        var actualResult = provcessor.process(context, observer, ResponseLineProcessor.DATA_LINE_PREFIX + "Abc");

        // Then
        Assert.assertTrue(actualResult);
        verify(context, times(0)).acceptAndGetLength("Xyz");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessWhenHasGeneratedText()
    {
        // Given
        var provcessor = createInstance(1);
        var response = new AIResponse();
        response.setGeneratedText("Asd");
        var token = new AIResponse.Token();
        token.setText("Xyz");
        response.setToken(token);

        // When
        when(json.deserialize("Abc", AIResponse.class)).thenReturn(response);
        when(context.acceptAndGetLength("Asd")).thenReturn(2);
        when(observer.onNext("As")).thenReturn(true);
        var actualResult = provcessor.process(context, observer, ResponseLineProcessor.DATA_LINE_PREFIX + "Abc");

        // Then
        Assert.assertFalse(actualResult);
        verify(observer).onNext("As");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessWhenHasGeneratedTextAndCompleted()
    {
        // Given
        var provcessor = createInstance(1);
        var response = new AIResponse();
        response.setGeneratedText("Asd");
        var token = new AIResponse.Token();
        token.setText("Xyz");
        response.setToken(token);

        // When
        when(json.deserialize("Abc", AIResponse.class)).thenReturn(response);
        when(context.acceptAndGetLength("Asd")).thenReturn(3);
        when(observer.onNext("Asd")).thenReturn(true);
        var actualResult = provcessor.process(context, observer, ResponseLineProcessor.DATA_LINE_PREFIX + "Abc");

        // Then
        Assert.assertFalse(actualResult);
        verify(observer).onNext("Asd");
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotProcessWhenHasEmptyGeneratedTextAndCompleted()
    {
        // Given
        var provcessor = createInstance(1);
        var response = new AIResponse();
        response.setGeneratedText("");
        var token = new AIResponse.Token();
        token.setText("Xyz");
        response.setToken(token);

        // When
        when(json.deserialize("Abc", AIResponse.class)).thenReturn(response);
        var actualResult = provcessor.process(context, observer, ResponseLineProcessor.DATA_LINE_PREFIX + "Abc");

        // Then
        Assert.assertFalse(actualResult);
        verify(context, times(0)).acceptAndGetLength("");
    }

    private ResponseLineProcessor createInstance(int codeCompletionLinesCount)
    {
        return new ResponseLineProcessor(json);
    }
}
