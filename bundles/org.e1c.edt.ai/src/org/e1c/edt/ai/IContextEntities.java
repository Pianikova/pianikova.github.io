/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.time.Duration;

import org.e1c.edt.ai.assistent.model.ChatContext;
import org.e1c.edt.ai.assistent.model.LocalContext;

public interface IContextEntities
{
    Duration fill(AIContext aiContext, LocalContext context, IStatistics statistics,
        ICancellationToken cancellationToken);

    void fill(AIContext aiContext, ChatContext context, IStatistics statistics,
        ICancellationToken cancellationToken);
}
