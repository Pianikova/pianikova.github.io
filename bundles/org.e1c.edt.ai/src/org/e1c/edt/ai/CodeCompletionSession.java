/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CodeCompletionSession<TContext extends ICodeCompletionContext>
    implements ICodeCompletionSession<TContext>
{
    private final IHistoricalHint hint;
    private TContext context;
    private IHintHistory history;
    private boolean singleWordMode;
    private boolean isAccepting, inDone;
    private String uuid = Sources.UNKNOWN.getId();
    private CodeMethod method = Sources.UNKNOWN.getMethod();

    @Inject
    public CodeCompletionSession(IHistoricalHint hint, IHintHistory history)
    {
        Preconditions.checkNotNull(hint);
        Preconditions.checkNotNull(history);
        this.hint = hint;
        this.history = history;
    }

    @Override
    public ICodeCompletionSession<TContext> initiaize(TContext context, IHintHistory history,
        int codeCompletionLinesCount, boolean singleWordMode)
    {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(hint);
        Preconditions.checkNotNull(hint);
        this.context = context;
        this.history = history;
        this.singleWordMode = singleWordMode;
        hint.initiaize(history, singleWordMode ? 1 : codeCompletionLinesCount, singleWordMode);
        return this;
    }

    @Override
    public String getId()
    {
        return uuid;
    }

    @Override
    public void setId(String uuid)
    {
        Preconditions.checkNotNull(uuid);
        Preconditions.checkArgument(!uuid.isBlank());
        this.uuid = uuid;
    }

    @Override
    public CodeMethod getMethod()
    {
        return method;
    }

    @Override
    public void setMethod(CodeMethod method)
    {
        Preconditions.checkNotNull(method);
        this.method = method;
    }

    @Override
    public synchronized TContext getContext()
    {
        if (context == null)
        {
            throw new IllegalStateException("Not initialized."); //$NON-NLS-1$
        }

        return context;
    }

    @Override
    public synchronized IHint getHint()
    {
        return hint;
    }

    @Override
    public synchronized boolean isAccepting()
    {
        return isAccepting;
    }

    @Override
    public synchronized IHistoricalHint getHistHint()
    {
        return hint;
    }

    @Override
    public synchronized boolean isDone()
    {
        return inDone && hint.isEmpty();
    }

    @Override
    public synchronized void complete()
    {
        inDone = true;
    }

    @Override
    public synchronized CodeCompletionAction finish()
    {
        return CodeCompletionAction.RESET;
    }

    @Override
    public synchronized CodeCompletionAction accept(HintPart part, int offset)
    {
        if (getHint().isEmpty())
        {
            return history.isEmpty() ? CodeCompletionAction.SKIP : CodeCompletionAction.HANDLE;
        }

        var text = hint.pull(part);
        apply(text, offset);
        if (isFinishingChar())
        {
            return CodeCompletionAction.RESET;
        }

        return CodeCompletionAction.UPDATE;
    }

    @Override
    public synchronized CodeCompletionAction acceptChar(int offset, char ch)
    {
        if (ch == 0)
        {
            return CodeCompletionAction.SKIP;
        }

        if (ch == '\b')
        {
            return CodeCompletionAction.RESET;
        }

        var charText = hint.pullChar(ch);
        if (charText.isEmpty())
        {
            return CodeCompletionAction.ASK_NEW;
        }

        apply(charText, offset);
        if (isFinishingChar())
        {
            return CodeCompletionAction.RESET;
        }

        return CodeCompletionAction.UPDATE;
    }

    @Override
    public synchronized CodeCompletionAction rollback(int offset)
    {
        var text = hint.rollback();
        if (text.isEmpty())
        {
            return CodeCompletionAction.RESET;
        }

        rollback(text, offset);
        return CodeCompletionAction.UPDATE;
    }

    @Override
    public synchronized void reset()
    {
        inDone = false;
        hint.clear();
    }

    private void rollback(Text text, int offset)
    {
        var hintText = text.getText();
        var length = hintText.length();
        if (length == 0)
        {
            return;
        }

        try
        {
            isAccepting = true;
            var start = offset - length;
            if (offset < 0)
            {
                start = 0;
            }

            getContext().rollback(start, length);
        }
        finally
        {
            isAccepting = false;
        }
    }

    private void apply(Text text, int offset)
    {
        var hintText = text.getText();
        var len = hintText.length();
        if (len == 0)
        {
            return;
        }

        try
        {
            isAccepting = true;
            var start = offset;
            if (offset < 0)
            {
                start = 0;
            }

            getContext().apply(text, start);
        }
        finally
        {
            isAccepting = false;
        }
    }

    private boolean isFinishingChar()
    {
        if (!singleWordMode)
        {
            return false;
        }

        var hint = getHint();
        return hint.isBlank();
    }
}