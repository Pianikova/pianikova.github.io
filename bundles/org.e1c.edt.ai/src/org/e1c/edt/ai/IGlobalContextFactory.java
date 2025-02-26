/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import org.e1c.edt.ai.assistent.model.GlobalContext;

public interface IGlobalContextFactory
{
    GlobalContext createGlobalContext(AIContext aiContext, IStatistics statistics,
        ICancellationToken cancellationToken);
}
