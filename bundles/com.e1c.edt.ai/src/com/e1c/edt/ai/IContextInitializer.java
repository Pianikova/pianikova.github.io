/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

public interface IContextInitializer
{
    Optional<AIContext> initialize(AIContext ctx, boolean limitSize);
}
