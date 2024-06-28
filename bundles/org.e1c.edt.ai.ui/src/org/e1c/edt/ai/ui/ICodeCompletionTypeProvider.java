/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.CodeCompletionType;

public interface ICodeCompletionTypeProvider
{
    CodeCompletionType getType(AISourceContext ctx);
}
