/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.stream.Stream;

import org.e1c.edt.ai.CancellationToken;
import org.e1c.edt.ai.IObserver;

public interface IResponseStreamProcessor
{
    void process(Stream<String> responseStream, IObserver<String> observer, CancellationToken cancellationToken);
}
