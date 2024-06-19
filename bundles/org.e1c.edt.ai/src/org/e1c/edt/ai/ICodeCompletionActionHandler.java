/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface ICodeCompletionActionHandler<TContext extends ICodeCompletionContext>
{
    CodeCompletionAction handle(ICodeCompletionSession<TContext> session,
        CodeCompletionAction action, Character character, int offset, boolean isContinuousCodeCompletion);
}
