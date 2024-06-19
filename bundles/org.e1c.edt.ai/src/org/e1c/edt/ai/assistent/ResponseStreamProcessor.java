/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IObserver;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ResponseStreamProcessor implements IResponseStreamProcessor
{
    private final IResponseLineProcessor lineProcessor;

    @Inject
    public ResponseStreamProcessor(
        IResponseLineProcessor lineProcessor)
    {
        Preconditions.checkNotNull(lineProcessor);
        this.lineProcessor = lineProcessor;
    }

    @Override
    public void process(Stream<String> stream, IObserver<String> observer, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(stream);
        Preconditions.checkNotNull(observer);
        Preconditions.checkNotNull(cancellationToken);
        stream.takeWhile(line -> {
            try
            {
                return !cancellationToken.isCanceled() && lineProcessor.process(observer, line);
            }
            catch (Throwable error)
            {
                observer.onError(error);
                return false;
            }
        }).collect(Collectors.toList());
    }
}