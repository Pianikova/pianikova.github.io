/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.time.Duration;

import org.e1c.edt.ai.assistent.model.LocalContext;

public interface IContextEntities
{
    Duration fill(AIContext aiContext, LocalContext context, ICancellationToken cancellationToken);
}
