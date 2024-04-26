/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

public interface IAIContextProvider
{
    Optional<AIContext> create();
}
