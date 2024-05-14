/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.e1c.edt.ai.CancellationToken;
import org.e1c.edt.ai.IObserver;

public class ResponseStreamProcessor implements IResponseStreamProcessor
{
    private final IResponseLineProcessor lineProcessor;

    public ResponseStreamProcessor(
        IResponseLineProcessor lineProcessor)
    {
        this.lineProcessor = lineProcessor;
    }

    @Override
    public void process(Stream<String> stream, IObserver<String> observer, CancellationToken cancellationToken)
    {
        try (stream)
        {
            stream.takeWhile(line -> !cancellationToken.isCanceled() && lineProcessor.process(observer, line))
                .collect(Collectors.toList());

            observer.onCompleted();
        }
        catch (Throwable e)
        {
            observer.onError(e);
        }
    }
}