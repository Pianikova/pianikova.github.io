/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CodeCompletionSession<TContext extends ICodeCompletionContext>
    implements ICodeCompletionSession<TContext>
{
    private final IUISettings settings;
    private final IHistoricalHint hint;
    private IHintHistory history;
    private TContext context;
    private boolean singleWordMode;
    private boolean isAccepting, inCompleted;
    private String uuid = Sources.UNKNOWN.getId();
    private CodeMethod method = Sources.UNKNOWN.getMethod();

    @Inject
    public CodeCompletionSession(IUISettings settings, IHistoricalHint hint, IHintHistory history)
    {
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(hint);
        Preconditions.checkNotNull(history);
        this.settings = settings;
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
    public synchronized boolean isDone()
    {
        return isСompleted() && hint.isEmpty();
    }

    @Override
    public synchronized boolean isСompleted()
    {
        return inCompleted;
    }

    @Override
    public synchronized void complete()
    {
        inCompleted = true;
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
        switch (ch)
        {
        case 0:
            return CodeCompletionAction.SKIP;

        case '\b':
            return CodeCompletionAction.RESET;

        case '\r':
        case '\n':
            var lineSeparator = settings.getLineSeparator();
            var charText = Text.EMPTY;
            for (var i = 0; i < lineSeparator.length(); i++)
            {
                charText = hint.pullChar(lineSeparator.charAt(i));
                if (charText.isEmpty())
                {
                    return CodeCompletionAction.ASK_NEW;
                }
            }

            apply(new Text(lineSeparator, charText.getSource()), offset);
            break;

        default:
            charText = hint.pullChar(ch);
            if (charText.isEmpty())
            {
                return CodeCompletionAction.ASK_NEW;
            }

            apply(charText, offset);
            break;
        }

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
        inCompleted = false;
        hint.clear();
    }

    private void rollback(Text text, int offset)
    {
        var hintText = text.getText();
        if (hintText.length() == 1)
        {
            var lineSeparator = settings.getLineSeparator();
            if (lineSeparator.length() > 1 && hintText.charAt(0) == lineSeparator.charAt(lineSeparator.length() - 1))
            {
                hintText = lineSeparator;
                hint.rollback();
            }
        }

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
        return hint.isEmpty();
    }
}