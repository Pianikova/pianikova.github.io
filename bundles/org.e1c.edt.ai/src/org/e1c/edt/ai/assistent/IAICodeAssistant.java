/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IObservable;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IAICodeAssistant
{
    public IObservable<String> generate(AIContext aiContext, ICancellationToken cancellationToken);
}
