/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;

public interface IAIContextProvider
{
    Optional<AIContext> create(AITarget target, ICancellationToken cancellationToken);
}
