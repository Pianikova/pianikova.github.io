/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.stream.Stream;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.assistent.model.Completion;

public interface IResponseStreamProcessor
{
    void process(Stream<String> responseStream, IObserver<Completion> observer,
        ICancellationToken cancellationToken);
}
