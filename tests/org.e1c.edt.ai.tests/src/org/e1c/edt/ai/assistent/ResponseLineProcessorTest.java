/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.assistent.model.Completion;
import org.e1c.edt.ai.assistent.model.CompletionResponse;
import org.junit.Assert;
import org.junit.Test;

public class ResponseLineProcessorTest
{
    private final IJson json = mock(IJson.class);
    private final ITextPreprocessor textPreprocessor = mock(ITextPreprocessor.class);
    @SuppressWarnings("unchecked")
    private final IObserver<Completion> observer = mock(IObserver.class);

    public ResponseLineProcessorTest()
    {
        when(textPreprocessor.process(anyString())).then(i -> i.getArgument(0));
    }

    @Test
    public void shouldProcessNullLine()
    {
        // Given
        var processor = createInstance(1);

        // When
        var actualResult = processor.process(observer, null);

        // Then
        Assert.assertTrue(actualResult);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessEmptyLine()
    {
        // Given
        var processor = createInstance(1);

        // When
        var actualResult = processor.process(observer, "");

        // Then
        Assert.assertTrue(actualResult);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcess()
    {
        // Given
        var processor = createInstance(1);

        var data = new Completion();
        data.text = "Xyz";
        data.uuid = "123";

        var response = new CompletionResponse();
        response.data = data;

        // When
        when(json.deserialize("{Abc}", CompletionResponse.class)).thenReturn(Optional.of(response));
        var actualResult = processor.process(observer, "Abc");

        // Then
        Assert.assertTrue(actualResult);
        verify(observer).onNext(data);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessUsingPreprocessor()
    {
        // Given
        var processor = createInstance(1);

        var data = new Completion();
        data.text = "Asd";
        data.uuid = "123";

        var response = new CompletionResponse();
        response.data = data;

        // When
        when(textPreprocessor.process("Xyz")).thenReturn("Asd");
        when(json.deserialize("{Abc}", CompletionResponse.class)).thenReturn(Optional.of(response));
        var actualResult = processor.process(observer, "Abc");

        // Then
        Assert.assertTrue(actualResult);
        verify(observer).onNext(data);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessWhenHasFinishReason()
    {
        // Given
        var processor = createInstance(1);

        var data = new Completion();
        data.text = "Xyz";
        data.finishReason = "stop";
        data.uuid = "123";

        var response = new CompletionResponse();
        response.data = data;

        // When
        when(json.deserialize("{Abc}", CompletionResponse.class)).thenReturn(Optional.of(response));
        var actualResult = processor.process(observer, "Abc");

        // Then
        Assert.assertFalse(actualResult);
        verify(observer).onNext(data);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessWhenHasEmptyLineFinishReason()
    {
        // Given
        var processor = createInstance(1);

        var data = new Completion();
        data.text = "";
        data.finishReason = "stop";
        data.uuid = "";

        var response = new CompletionResponse();
        response.data = data;

        // When
        when(json.deserialize("{Abc}", CompletionResponse.class)).thenReturn(Optional.of(response));
        var actualResult = processor.process(observer, "Abc");

        // Then
        Assert.assertFalse(actualResult);
        verify(observer, never()).onNext(data);
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessWhenDataIsNull()
    {
        // Given
        var processor = createInstance(1);

        // When
        var actualResult = processor.process(observer, "Abc");

        // Then
        Assert.assertTrue(actualResult);
    }


    @SuppressWarnings("nls")
    @Test
    public void shouldHandleException()
    {
        // Given
        var processor = createInstance(1);
        var data = new Completion();
        data.text = "Xyz";
        data.uuid = "123";

        var response = new CompletionResponse();
        response.data = data;
        // When
        when(json.deserialize("{Abc}", CompletionResponse.class)).thenThrow(NullPointerException.class);
        var actualResult = processor.process(observer, "Abc");

        // Then
        Assert.assertFalse(actualResult);
    }

    private ResponseLineProcessor createInstance(int codeCompletionLinesCount)
    {
        return new ResponseLineProcessor(json, textPreprocessor);
    }
}
