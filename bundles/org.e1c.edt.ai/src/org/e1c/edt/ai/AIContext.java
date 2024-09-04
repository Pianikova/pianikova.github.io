/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;

public class AIContext
{
    private final String source;
    private final int sourceOffset;
    private final String path;
    private final int textOffset;
    private final String text;
    private final String prefix;
    private final String sufix;
    private final int start;
    private final int finish;

    public AIContext(String source, int sourceOffset, String path, String text, int textOffset,
        String prefix,
        String sufix, int start, int finish)
    {
        Preconditions.checkNotNull(source);
        Preconditions.checkArgument(sourceOffset >= 0);
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(textOffset >= 0);
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(sufix);
        this.source = source;
        this.sourceOffset = sourceOffset;
        this.path = path;
        this.textOffset = textOffset;
        this.text = text;
        this.prefix = prefix;
        this.sufix = sufix;
        this.start = start;
        this.finish = finish;
    }

    public AIContext(String source, int sourceOffset, String path, String text, int textOffset)
    {
        this(source, sourceOffset, path, text, textOffset, "", "", 0, 0); //$NON-NLS-1$//$NON-NLS-2$
    }

    public String getSource()
    {
        return source;
    }

    public int getSourceOffset()
    {
        return sourceOffset;
    }

    public String getPath()
    {
        return path;
    }

    public String getText()
    {
        return text;
    }

    public int getTextOffset()
    {
        return textOffset;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public String getSufix()
    {
        return sufix;
    }

    public int getStart()
    {
        return start;
    }

    public int getFinish()
    {
        return finish;
    }

    @Override
    public String toString()
    {
        var str = new StringBuilder();

        str.append("path:"); //$NON-NLS-1$
        str.append(path);
        str.append(System.lineSeparator());

        str.append("cursorOffset:"); //$NON-NLS-1$
        str.append(textOffset);
        str.append(System.lineSeparator());

        str.append("start:"); //$NON-NLS-1$
        str.append(getStart());
        str.append(System.lineSeparator());

        str.append("finish:"); //$NON-NLS-1$
        str.append(getFinish());
        str.append(System.lineSeparator());

        var textWithCursor = text.substring(0, textOffset) + "█" + text.substring(textOffset); //$NON-NLS-1$

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
}