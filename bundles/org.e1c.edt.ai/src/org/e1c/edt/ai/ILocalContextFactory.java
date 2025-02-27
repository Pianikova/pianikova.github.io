/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import org.e1c.edt.ai.assistent.model.LocalContext;

public interface ILocalContextFactory
{
    LocalContext createLocalContext(AIContext aiContext, IStatistics statistics, ICancellationToken cancellationToken);
}
