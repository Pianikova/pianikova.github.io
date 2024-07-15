/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.assistent.ResponseLineProcessor;
import org.e1c.edt.ai.assistent.model.CompletionResponse;
import org.e1c.edt.ai.assistent.model.Completion;
import org.junit.Assert;
import org.junit.Test;

public class ResponseLineProcessorTest
{
    private final IJson json = mock(IJson.class);
    @SuppressWarnings("unchecked")
    private final IObserver<Completion> observer = mock(IObserver.class);

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
    public void shouldProcess()
    {
        // Given
        var provcessor = createInstance(1);

        var data = new Completion();
        data.text = "Xyz";
        data.uuid = "123";

        var response = new CompletionResponse();
        response.data = data;

        // When
        when(json.deserialize("{Abc}", CompletionResponse.class)).thenReturn(Optional.of(response));
        var actualResult = provcessor.process(observer, "Abc");

        // Then
        Assert.assertTrue(actualResult);
        verify(observer).onNext(data);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessWhenHasFinishReason()
    {
        // Given
        var provcessor = createInstance(1);

        var data = new Completion();
        data.text = "Xyz";
        data.finishReason = "stop";
        data.uuid = "123";

        var response = new CompletionResponse();
        response.data = data;

        // When
        when(json.deserialize("{Abc}", CompletionResponse.class)).thenReturn(Optional.of(response));
        var actualResult = provcessor.process(observer, "Abc");

        // Then
        Assert.assertFalse(actualResult);
        verify(observer).onNext(data);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessWhenHasEmptyLineFinishReason()
    {
        // Given
        var provcessor = createInstance(1);

        var data = new Completion();
        data.text = "";
        data.finishReason = "stop";
        data.uuid = "";

        var response = new CompletionResponse();
        response.data = data;

        // When
        when(json.deserialize("{Abc}", CompletionResponse.class)).thenReturn(Optional.of(response));
        var actualResult = provcessor.process(observer, "Abc");

        // Then
        Assert.assertFalse(actualResult);
        verify(observer, never()).onNext(data);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessWhenDataIsNull()
    {
        // Given
        var provcessor = createInstance(1);

        var response = new CompletionResponse();

        // When
        var actualResult = provcessor.process(observer, "Abc");

        // Then
        Assert.assertTrue(actualResult);
    }


    @SuppressWarnings("nls")
    @Test
    public void shouldHandleException()
    {
        // Given
        var provcessor = createInstance(1);
        var data = new Completion();
        data.text = "Xyz";
        data.uuid = "123";

        var response = new CompletionResponse();
        response.data = data;
        // When
        when(json.deserialize("{Abc}", CompletionResponse.class)).thenThrow(NullPointerException.class);
        var actualResult = provcessor.process(observer, "Abc");

        // Then
        Assert.assertFalse(actualResult);
    }

    private ResponseLineProcessor createInstance(int codeCompletionLinesCount)
    {
        return new ResponseLineProcessor(json);
    }
}
