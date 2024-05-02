/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.e1c.edt.ai.IObserver;

public class ResponseStreamProcessor implements IResponseStreamProcessor
{
    private final Supplier<IResponseStreamContext> contextFactory;
    private final IResponseLineProcessor lineProcessor;

    public ResponseStreamProcessor(Supplier<IResponseStreamContext> contextFactory,
        IResponseLineProcessor lineProcessor)
    {
        this.contextFactory = contextFactory;
        this.lineProcessor = lineProcessor;
    }

    @Override
    public void process(Stream<String> stream, IObserver<String> observer, CancellationToken cancellationToken)
    {
        try (stream)
        {
            var context = contextFactory.get();
            stream.takeWhile(line -> !cancellationToken.isCanceled() && lineProcessor.process(context, observer, line))
                .collect(Collectors.toList());

            observer.onCompleted();
        }
        catch (Throwable e)
        {
            observer.onError(e);
        }
    }
}