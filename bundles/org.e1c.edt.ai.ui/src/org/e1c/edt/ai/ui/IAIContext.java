/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

public interface IAIContext
{
    Optional<AIContext> create();
}
