/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.itests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.assistent.CancellationToken;
import org.e1c.edt.ai.assistent.IResponseLineProcessor;
import org.e1c.edt.ai.assistent.ResponseStreamProcessor;
import org.junit.Test;

public class ResponseStreamProcessorTest
{
    private final IResponseLineProcessor lineProcessor = mock(IResponseLineProcessor.class);
    @SuppressWarnings("unchecked")
    private final IObserver<String> observer = mock(IObserver.class);

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessWhileNotAborted()
    {
        // Given
        var provcessor = createInstance(1);
        var data = List.of("Abc", "Xy", "Asd", "Rty");

        // When
        when(lineProcessor.process(observer, "Abc")).thenReturn(true);
        when(lineProcessor.process(observer, "Xy")).thenReturn(true);
        when(lineProcessor.process(observer, "Asd")).thenReturn(false);
        provcessor.process(data.stream(), observer, new CancellationToken());

        // Then
        verify(lineProcessor).process(observer, "Abc");
        verify(lineProcessor).process(observer, "Xy");
        verify(lineProcessor).process(observer, "Asd");
        verify(lineProcessor, times(0)).process(observer, "Rty");
        verify(observer).onCompleted();
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessAllIfNotAborted()
    {
        // Given
        var provcessor = createInstance(1);
        var data = List.of("Abc", "Xy", "Asd", "Rty");

        // When
        when(lineProcessor.process(observer, "Abc")).thenReturn(true);
        when(lineProcessor.process(observer, "Xy")).thenReturn(true);
        when(lineProcessor.process(observer, "Asd")).thenReturn(true);
        when(lineProcessor.process(observer, "Rty")).thenReturn(true);
        provcessor.process(data.stream(), observer, new CancellationToken());

        // Then
        verify(lineProcessor).process(observer, "Abc");
        verify(lineProcessor).process(observer, "Xy");
        verify(lineProcessor).process(observer, "Asd");
        verify(lineProcessor).process(observer, "Rty");
        verify(observer).onCompleted();
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotProcessWhenCancaled()
    {
        // Given
        var provcessor = createInstance(1);
        var data = List.of("Abc", "Xy", "Asd", "Rty");

        // When
        var cancellationToken = new CancellationToken();
        cancellationToken.cancel();
        when(lineProcessor.process(observer, "Abc")).thenReturn(true);
        when(lineProcessor.process(observer, "Xy")).thenReturn(true);
        when(lineProcessor.process(observer, "Asd")).thenReturn(false);
        provcessor.process(data.stream(), observer, cancellationToken);

        // Then
        verify(lineProcessor, times(0)).process(observer, "Abc");
        verify(lineProcessor, times(0)).process(observer, "Xy");
        verify(lineProcessor, times(0)).process(observer, "Asd");
        verify(lineProcessor, times(0)).process(observer, "Rty");
        verify(observer).onCompleted();
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldPublishError()
    {
        // Given
        var provcessor = createInstance(1);
        var data = List.of("Abc", "Xy", "Asd", "Rty");
        var error = new Throwable();

        // When
        when(lineProcessor.process(observer, "Abc")).thenReturn(true);
        when(lineProcessor.process(observer, "Xy")).thenAnswer(i -> {
            throw error;
        });
        provcessor.process(data.stream(), observer, new CancellationToken());

        // Then
        verify(lineProcessor).process(observer, "Abc");
        verify(lineProcessor).process(observer, "Xy");
        verify(lineProcessor, times(0)).process(observer, "Asd");
        verify(observer).onError(error);
        verify(observer, times(0)).onCompleted();
    }

    private ResponseStreamProcessor createInstance(int codeCompletionLinesCount)
    {
        return new ResponseStreamProcessor(lineProcessor);
    }
}
