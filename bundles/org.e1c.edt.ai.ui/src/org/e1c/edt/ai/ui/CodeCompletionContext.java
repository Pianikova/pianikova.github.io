/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationTokenSource;
import org.e1c.edt.ai.ICodeCompletionContext;
import org.e1c.edt.ai.Text;
import org.eclipse.swt.custom.StyledText;

import com.google.common.base.Preconditions;

public class CodeCompletionContext
    implements ICodeCompletionContext
{
    private final ICodeCompletionContext baseContext;
    private final AIContext aiContext;
    private final StyledText textWidget;
    private final CancellationTokenSource cancellationTokenSource;

    public CodeCompletionContext(ICodeCompletionContext baseContext, AIContext aiContext, StyledText textWidget,
        CancellationTokenSource cancellationTokenSource)
    {
        Preconditions.checkNotNull(baseContext);
        Preconditions.checkNotNull(aiContext);
        Preconditions.checkNotNull(textWidget);
        Preconditions.checkNotNull(cancellationTokenSource);
        this.baseContext = baseContext;
        this.aiContext = aiContext;
        this.textWidget = textWidget;
        this.cancellationTokenSource = cancellationTokenSource;
    }

    public boolean isSingleWordMode()
    {
        var offset = textWidget.getCaretOffset();
        var contet = textWidget.getContent();
        var contentLength = contet.getCharCount();
        var prefix = contet.getTextRange(offset, contentLength - offset);
        var pos = 0;
        while (pos < prefix.length())
        {
            var ch = prefix.charAt(pos);
            if (!Character.isWhitespace(ch))
            {
                return true;
            }

            if (ch == '\n')
            {
                return false;
            }

            pos++;
        }

        return false;
    }

    @Override
    public void apply(Text text, int offset)
    {
        Preconditions.checkNotNull(text);
        replace(offset, 0, text.getText());
        baseContext.apply(text, offset);
    }

    @Override
    public void rollback(int offset, int length)
    {
        replace(offset, length, ""); //$NON-NLS-1$
        baseContext.rollback(offset, length);
    }

    @Override
    public void commit(String lastSourceId, int lastOffset)
    {
        baseContext.commit(lastSourceId, lastOffset);
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

    private void replace(int start, int replaceLength, String text)
    {
        var contet = textWidget.getContent();
        var contentLength = contet.getCharCount();
        if (start > contentLength)
        {
            start = contentLength;
        }

        contet.replaceTextRange(start, replaceLength, text);
        textWidget.setCaretOffset(start + text.length());
        textWidget.showSelection();
    }
}
