/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.assistent.model.Completion;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ResponseStreamProcessor
    implements IResponseStreamProcessor
{
    private final IThreadManager threadManager;
    private final IResponseLineProcessor lineProcessor;

    @Inject
    public ResponseStreamProcessor(
        IThreadManager threadManager,
        IResponseLineProcessor lineProcessor)
    {
        Preconditions.checkNotNull(threadManager);
        Preconditions.checkNotNull(lineProcessor);
        this.threadManager = threadManager;
        this.lineProcessor = lineProcessor;
    }

    @Override
    public void process(Stream<String> stream, IObserver<Completion> observer, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(stream);
        Preconditions.checkNotNull(observer);
        Preconditions.checkNotNull(cancellationToken);
        stream.takeWhile(line -> process(observer, cancellationToken, line)).collect(Collectors.toList());
    }

    private boolean process(IObserver<Completion> observer, ICancellationToken cancellationToken, String line)
    {
        if (cancellationToken.isCanceled())
        {
            threadManager.cancel();
        }

        try
        {
            return lineProcessor.process(observer, line);
        }
        catch (Throwable error)
        {
            observer.onError(error);
            return false;
        }
    }
}