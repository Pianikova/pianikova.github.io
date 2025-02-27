/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class Range
{
    public final static Range EMPTY = new Range(0, 0);
    private int start;
    private int length;

    public Range(int start, int length)
    {
        Preconditions.checkArgument(start >= 0);
        Preconditions.checkArgument(length >= 0);
        this.start = start;
        this.length = length;
    }

    public int getStart()
    {
        return start;
    }

    public int getLength()
    {
        return length;
    }

    public boolean isEmpty()
    {
        return length <= 0;
    }

    public String apply(String text)
    {
        Preconditions.checkNotNull(text);
        var endIndex = getStart() + getLength();
        Preconditions.checkArgument(endIndex <= text.length());
        return text.substring(getStart(), endIndex);
    }

    public boolean contains(int position)
    {
        return position >= start && position < start + length;
    }

    public Range merge(Range range)
    {
        var start = Integer.min(getStart(), range.getStart());
        var finish = Integer.max(getStart() + getLength(), range.getStart() + range.getLength());
        return new Range(start, finish - start);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(length, start);
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
        Range other = (Range)obj;
        return length == other.length && start == other.start;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "[" + start + ", " + length + "]";
    }
}
