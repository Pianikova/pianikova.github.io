/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CancellationException;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.assistent.model.Completion;

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
        var processor = createInstance(1);
        var data = List.of("Abc", "Xy", "Asd", "Rty");

        // When
        when(lineProcessor.process(observer, "Abc", CancellationTokens.NONE)).thenReturn(true);
        when(lineProcessor.process(observer, "Xy", CancellationTokens.NONE)).thenReturn(true);
        when(lineProcessor.process(observer, "Asd", CancellationTokens.NONE)).thenReturn(false);
        processor.process(data.stream(), observer, CancellationTokens.NONE);

        // Then
        verify(lineProcessor).process(observer, "Abc", CancellationTokens.NONE);
        verify(lineProcessor).process(observer, "Xy", CancellationTokens.NONE);
        verify(lineProcessor).process(observer, "Asd", CancellationTokens.NONE);
        verify(lineProcessor, times(0)).process(observer, "Rty", CancellationTokens.NONE);
        verify(observer, times(0)).onCompleted();
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldProcessAllIfNotAborted()
    {
        // Given
        var processor = createInstance(1);
        var data = List.of("Abc", "Xy", "Asd", "Rty");

        // When
        when(lineProcessor.process(observer, "Abc", CancellationTokens.NONE)).thenReturn(true);
        when(lineProcessor.process(observer, "Xy", CancellationTokens.NONE)).thenReturn(true);
        when(lineProcessor.process(observer, "Asd", CancellationTokens.NONE)).thenReturn(true);
        when(lineProcessor.process(observer, "Rty", CancellationTokens.NONE)).thenReturn(true);
        processor.process(data.stream(), observer, CancellationTokens.NONE);

        // Then
        verify(lineProcessor).process(observer, "Abc", CancellationTokens.NONE);
        verify(lineProcessor).process(observer, "Xy", CancellationTokens.NONE);
        verify(lineProcessor).process(observer, "Asd", CancellationTokens.NONE);
        verify(lineProcessor).process(observer, "Rty", CancellationTokens.NONE);
        verify(observer, times(0)).onCompleted();
    }

    @SuppressWarnings("nls")
    @Test
    public void shouldNotProcessWhenCancaled()
    {
        // Given
        Mockito.doThrow(new CancellationException()).when(threadManager).cancel();
        var processor = createInstance(1);
        var data = List.of("Abc", "Xy", "Asd", "Rty");

        // When
        final var cancellationTokenSource = new CancellationTokenSource();

        when(lineProcessor.process(observer, "Abc", cancellationTokenSource)).thenReturn(true);
        when(lineProcessor.process(observer, "Xy", cancellationTokenSource)).thenAnswer(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                cancellationTokenSource.cancel();
                return true;
            }

        });
        when(lineProcessor.process(observer, "Asd", cancellationTokenSource)).thenReturn(false);
        assertThrows(CancellationException.class,
            () -> processor.process(data.stream(), observer, cancellationTokenSource));

        // Then
        verify(threadManager).cancel();
        verify(lineProcessor, times(1)).process(observer, "Abc", cancellationTokenSource);
        verify(lineProcessor, times(1)).process(observer, "Xy", cancellationTokenSource);
        verify(lineProcessor, times(0)).process(observer, "Asd", cancellationTokenSource);
        verify(lineProcessor, times(0)).process(observer, "Rty", cancellationTokenSource);
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
        when(lineProcessor.process(observer, "Abc", CancellationTokens.NONE)).thenReturn(true);
        when(lineProcessor.process(observer, "Xy", CancellationTokens.NONE)).thenAnswer(i -> {
            throw error;
        });
        provcessor.process(data.stream(), observer, CancellationTokens.NONE);

        // Then
        verify(lineProcessor).process(observer, "Abc", CancellationTokens.NONE);
        verify(lineProcessor).process(observer, "Xy", CancellationTokens.NONE);
        verify(lineProcessor, times(0)).process(observer, "Asd", CancellationTokens.NONE);
        verify(observer).onError(error);
        verify(observer, times(0)).onCompleted();
    }

    private ResponseStreamProcessor createInstance(int codeCompletionLinesCount)
    {
        return new ResponseStreamProcessor(threadManager, lineProcessor);
    }
}
