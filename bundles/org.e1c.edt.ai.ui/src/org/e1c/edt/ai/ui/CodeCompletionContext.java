/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationTokenSource;
import org.e1c.edt.ai.ICodeCompletionContext;
import org.eclipse.swt.custom.StyledText;

import com.google.common.base.Preconditions;

public class CodeCompletionContext
    implements ICodeCompletionContext
{
    private final AIContext aiContext;
    private final StyledText textWidget;
    private final CancellationTokenSource cancellationTokenSource;

    public CodeCompletionContext(AIContext aiContext, StyledText textWidget,
        CancellationTokenSource cancellationTokenSource)
    {
        Preconditions.checkNotNull(aiContext);
        Preconditions.checkNotNull(textWidget);
        Preconditions.checkNotNull(cancellationTokenSource);
        this.aiContext = aiContext;
        this.textWidget = textWidget;
        this.cancellationTokenSource = cancellationTokenSource;
    }

    @Override
    public void replace(int start, int replaceLength, String text)
    {
        var contet = textWidget.getContent();
        var contentLength = contet.getCharCount();
        if (start > contentLength)
        {
            start = contentLength;
        }

        contet.replaceTextRange(start, replaceLength, text);
        textWidget.setCaretOffset(start + text.length());
    }

    public StyledText getWidget()
    {
        return textWidget;
    }

    public CancellationTokenSource getCancellationTokenSource()
    {
        return cancellationTokenSource;
    }

    public AIContext getAiContext()
    {
        return aiContext;
    }
}
