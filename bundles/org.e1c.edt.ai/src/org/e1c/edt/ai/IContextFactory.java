/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

public interface IContextFactory
{
    Optional<AIContext> create(String source, int sourceOffset, String text, int offset,
        CodeCompletionType codeCompletionType);
}
