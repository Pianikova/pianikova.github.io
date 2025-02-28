/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.GlobalContext;

public interface IGlobalContextFactory
{
    GlobalContext createGlobalContext(AIContext aiContext, IStatistics statistics,
        ICancellationToken cancellationToken);
}
