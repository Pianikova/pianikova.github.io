/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;

public interface IAIContextProvider
{
    Optional<AIContext> create(AITarget target, ICancellationToken cancellationToken);
}
