/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

public interface IContextInitializer
{
    Optional<AIContext> initialize(AIContext ctx);
}
