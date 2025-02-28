/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

public interface ICodeCompletionActionHandler<TContext extends ICodeCompletionContext>
{
    CodeCompletionAction handle(ICodeCompletionSession<TContext> session,
        CodeCompletionAction action, Character character, int offset, boolean isContinuousCodeCompletion);
}
