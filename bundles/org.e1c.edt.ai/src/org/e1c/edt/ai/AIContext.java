/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;

public class AIContext
{
    private final int cursorOffset;
    private final String text;
    private final CodeCompletionType complitionType;
    private final String prefix;
    private final String sufix;

    public AIContext(int cursorOffset, String text, CodeCompletionType complitionType, String prefix,
        String sufix)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(cursorOffset >= 0 && (text.isEmpty() || cursorOffset <= text.length()));
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(sufix);
        this.cursorOffset = cursorOffset;
        this.text = text;
        this.complitionType = complitionType;
        this.prefix = prefix;
        this.sufix = sufix;
    }

    public int getCursorOffset()
    {
        return cursorOffset;
    }

    public String getText()
    {
        return text;
    }

    @Override
    public String toString()
    {
        var str = new StringBuilder();

        str.append("cursorOffset:"); //$NON-NLS-1$
        str.append(cursorOffset);
        str.append(System.lineSeparator());

        str.append("complitionType:"); //$NON-NLS-1$
        str.append(complitionType);
        str.append(System.lineSeparator());

        var textWithCursor = text.substring(0, cursorOffset) + "█" + text.substring(cursorOffset); //$NON-NLS-1$

        str.append("text:"); //$NON-NLS-1$
        str.append(format(textWithCursor));
        str.append(System.lineSeparator());

        str.append("prefix:"); //$NON-NLS-1$
        str.append(format(prefix));
        str.append(System.lineSeparator());

        str.append("sufix:"); //$NON-NLS-1$
        str.append(format(sufix));
        str.append(System.lineSeparator());

        str.append("raw text:"); //$NON-NLS-1$
        str.append(System.lineSeparator());
        str.append(textWithCursor);

        return str.toString();
    }

    @SuppressWarnings("nls")
    private static String format(String text)
    {
        return "[" + text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "]";
    }

    public CodeCompletionType getComplitionType()
    {
        return complitionType;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public String getSufix()
    {
        return sufix;
    }
}