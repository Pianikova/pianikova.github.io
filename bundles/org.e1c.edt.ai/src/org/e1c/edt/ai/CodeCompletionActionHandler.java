/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public class CodeCompletionActionHandler<TContext extends ICodeCompletionContext> implements ICodeCompletionActionHandler<TContext>
{
    @Override
    public CodeCompletionAction handle(ICodeCompletionSession<TContext> session,
        CodeCompletionAction action, Character character, int offset, boolean isContinuousCodeCompletion)
    {
        if (action == CodeCompletionAction.SUGGEST)
        {
            return CodeCompletionAction.SUGGEST;
        }

        if (session != null && offset >= 0)
        {
            if (action == CodeCompletionAction.STOP)
            {
                return session.finish();
            }

            if (action == CodeCompletionAction.ROLLBACK_PART)
            {
                return session.rollback(offset);
            }

            if (action == CodeCompletionAction.ACCEPT_PART)
            {
                return session.accept(HintPart.TOKEN, offset);
            }

            if (action == CodeCompletionAction.ACCEPT)
            {
                return session.accept(HintPart.LINES, offset);
            }

            return session.acceptChar(offset, character);
        }

        var charType = Character.getType(character);
        if (charType != Character.CONTROL && !isContinuousCodeCompletion)
        {
            return CodeCompletionAction.RESET;
        }

        if (isContinuousCodeCompletion && character != '.'
            && (character == '\r' || character == '\n' || charType != Character.CONTROL))
        {
            return CodeCompletionAction.ASK_NEW;
        }

        return CodeCompletionAction.SKIP;
    }
}