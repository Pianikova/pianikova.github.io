/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CodeCompletionSession<TContext extends ICodeCompletionContext> implements ICodeCompletionSession<TContext>
{
    private final IUISettings uiSettings;
    private final IHistoricalHint hint;
    private TContext context;
    private IHintHistory history;
    private boolean isSingleWordMode;
    private boolean isAccepting, inDone;

    @Inject
    public CodeCompletionSession(IUISettings uiSettings, IHistoricalHint hint, IHintHistory history)
    {
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(hint);
        Preconditions.checkNotNull(history);
        this.uiSettings = uiSettings;
        this.hint = hint;
        this.history = history;
    }

    @Override
    public ICodeCompletionSession<TContext> initiaize(TContext context, IHintHistory history, boolean isSingleWordMode)
    {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(hint);
        Preconditions.checkNotNull(hint);
        this.context = context;
        this.history = history;
        this.isSingleWordMode = isSingleWordMode;
        hint.setMaxLines(isSingleWordMode ? 1 : uiSettings.getCodeCompletionLinesCount());
        hint.attachHistory(history);
        return this;
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
        isAccepting = false;
        hint.clear();
    }

    private void rollback(String hintText, int offset)
    {
        var len = hintText.length();
        if (len == 0)
        {
            return;
        }

        try
        {
            isAccepting = true;
            var start = offset - len;
            if (offset < 0)
            {
                start = 0;
            }

            getContext().replace(start, len, ""); //$NON-NLS-1$
        }
        finally
        {
            isAccepting = false;
        }
    }

    private void apply(String hintText, int offset)
    {
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

            getContext().replace(start, 0, hintText);
        }
        finally
        {
            isAccepting = false;
        }
    }

    private boolean isFinishingChar()
    {
        return isSingleWordMode && (getHint().isBlank() || getHint().startsWith('\n'));
    }
}