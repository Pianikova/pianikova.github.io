/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;

public class AIContext
{
    private final int cursorOffset;
    private final String text;
    private final String context;

    public AIContext(int cursorOffset, String text, String context)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(context);
        Preconditions.checkArgument(cursorOffset >= 0 && (text.isEmpty() || cursorOffset <= text.length()));
        this.cursorOffset = cursorOffset;
        this.text = text;
        this.context = context;
    }

    public int getCursorOffset()
    {
        return cursorOffset;
    }

    public String getText()
    {
        return text;
    }

    public String getContext()
    {
        return context;
    }

    @Override
    public String toString()
    {
        var str = new StringBuilder();

        str.append("cursorOffset:"); //$NON-NLS-1$
        str.append(cursorOffset);
        str.append(System.lineSeparator());

        var textWithCursor = text.substring(0, cursorOffset) + "█" + text.substring(cursorOffset); //$NON-NLS-1$

        str.append("text:"); //$NON-NLS-1$
        str.append(format(textWithCursor));
        str.append(System.lineSeparator());

        str.append("context:"); //$NON-NLS-1$
        str.append(format(context));

        return str.toString();
    }

    @SuppressWarnings("nls")
    private static String format(String text)
    {
        return "[" + text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "]";
    }
}