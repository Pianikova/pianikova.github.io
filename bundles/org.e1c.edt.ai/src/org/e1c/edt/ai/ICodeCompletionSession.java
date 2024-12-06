/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface ICodeCompletionSession<TContext extends ICodeCompletionContext>
    extends ISource
{
    ICodeCompletionSession<TContext> initiaize(TContext context, IHintHistory history, boolean singleWordMode);

    void setId(String uuid);

    void setMethod(CodeMethod method);

    TContext getContext();

    IHint getHint();

    boolean isAccepting();

    boolean isDone();

    CodeCompletionAction finish();

    CodeCompletionAction accept(HintPart part, int offset);

    CodeCompletionAction acceptChar(int offset, char ch);

    CodeCompletionAction rollback(int offset);

    void complete();

    void reset();

    IHistoricalHint getHistHint();
}