/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IObservable;
import org.e1c.edt.ai.assistent.model.Completion;

public interface ICodeAssistant
{
    public IObservable<Completion> createSource(ILocalContextProvider localContextProvider,
        ICancellationToken cancellationToken);
}
