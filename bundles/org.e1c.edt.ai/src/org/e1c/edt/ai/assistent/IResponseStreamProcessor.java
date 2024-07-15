/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.stream.Stream;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.assistent.model.Completion;

public interface IResponseStreamProcessor
{
    void process(Stream<String> responseStream, IObserver<Completion> observer,
        ICancellationToken cancellationToken);
}
