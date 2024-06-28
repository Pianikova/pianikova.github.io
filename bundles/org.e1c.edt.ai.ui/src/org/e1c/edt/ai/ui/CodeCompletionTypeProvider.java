/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.CodeCompletionType;

public class CodeCompletionTypeProvider implements ICodeCompletionTypeProvider
{
    @Override
    public CodeCompletionType getType(AISourceContext ctx)
    {
        return CodeCompletionType.CodeLines;
    }
}
