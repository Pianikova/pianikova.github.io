/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class Text
{
    public static final Text EMPTY = new Text("", Sources.UNKNOWN); //$NON-NLS-1$
    private final String text;
    private final ISource source;

    public Text(String text, ISource source)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(source);
        this.text = text;
        this.source = source;
    }

    public String getText()
    {
        return text;
    }

    public ISource getSource()
    {
        return source;
    }

    public boolean isEmpty()
    {
        return text.isEmpty();
    }

    @Override
    public String toString()
    {
        return text;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(source, text);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Text other = (Text)obj;
        return Objects.equals(source, other.source) && Objects.equals(text, other.text);
    }
}
