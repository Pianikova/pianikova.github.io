/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.assistent.model.Completion;

public interface IResponseLineProcessor
{
    boolean process(IObserver<Completion> observer, String line, ICancellationToken cancellationToken);
}
