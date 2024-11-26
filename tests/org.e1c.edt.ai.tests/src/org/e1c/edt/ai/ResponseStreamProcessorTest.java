/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CancellationException;

import org.e1c.edt.ai.assistent.IResponseLineProcessor;
import org.e1c.edt.ai.assistent.IThreadManager;
import org.e1c.edt.ai.assistent.ResponseStreamProcessor;
import org.e1c.edt.ai.assistent.model.Completion;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ResponseStreamProcessorTest
{
    private final IThreadManager threadManager = mock(IThreadManager.class);
    private final IResponseLineProcessor lineProcessor = mock(IResponseLineProcessor.class);
    @SuppressWarnings("unchecked")
    private final IObserver<Completion> observer = mock(IObserver.class);

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
        provcessor.process(data.stream(), observer, CancellationTokens.NONE);

        // Then
        verify(lineProcessor).process(observer, "Abc");
        verify(lineProcessor).process(observer, "Xy");
        verify(lineProcessor).process(observer, "Asd");
        verify(lineProcessor, times(0)).process(observer, "Rty");
        verify(observer, times(0)).onCompleted();
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
        provcessor.process(data.stream(), observer, CancellationTokens.NONE);

        // Then
        verify(lineProcessor).process(observer, "Abc");
        verify(lineProcessor).process(observer, "Xy");
        verify(lineProcessor).process(observer, "Asd");
        verify(lineProcessor).process(observer, "Rty");
        verify(observer, times(0)).onCompleted();
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotProcessWhenCancaled()
    {
        // Given
        Mockito.doThrow(new CancellationException()).when(threadManager).cancel();
        var provcessor = createInstance(1);
        var data = List.of("Abc", "Xy", "Asd", "Rty");

        // When
        final var cancellationTokenSource = new CancellationTokenSource();

        when(lineProcessor.process(observer, "Abc")).thenReturn(true);
        when(lineProcessor.process(observer, "Xy")).thenAnswer(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                cancellationTokenSource.cancel();
                return true;
            }

        });
        when(lineProcessor.process(observer, "Asd")).thenReturn(false);
        assertThrows(CancellationException.class,
            () -> provcessor.process(data.stream(), observer, cancellationTokenSource));

        // Then
        verify(threadManager).cancel();
        verify(lineProcessor, times(1)).process(observer, "Abc");
        verify(lineProcessor, times(1)).process(observer, "Xy");
        verify(lineProcessor, times(0)).process(observer, "Asd");
        verify(lineProcessor, times(0)).process(observer, "Rty");
        verify(observer, times(0)).onCompleted();
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
        provcessor.process(data.stream(), observer, CancellationTokens.NONE);

        // Then
        verify(lineProcessor).process(observer, "Abc");
        verify(lineProcessor).process(observer, "Xy");
        verify(lineProcessor, times(0)).process(observer, "Asd");
        verify(observer).onError(error);
        verify(observer, times(0)).onCompleted();
    }

    private ResponseStreamProcessor createInstance(int codeCompletionLinesCount)
    {
        return new ResponseStreamProcessor(threadManager, lineProcessor);
    }
}
