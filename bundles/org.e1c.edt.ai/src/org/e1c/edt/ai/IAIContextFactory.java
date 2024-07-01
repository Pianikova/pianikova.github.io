/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

public interface IAIContextFactory
{
    Optional<AIContext> create(String source, String text, int offset, CodeCompletionType codeCompletionType);
}
