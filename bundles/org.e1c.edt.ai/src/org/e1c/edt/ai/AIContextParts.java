/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class AIContextParts
{
    private final Range prefix;
    private final Range sufix;
    private final Range middle;

    public AIContextParts(Range prefix, Range sufix, Range middle)
    {
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(sufix);
        Preconditions.checkNotNull(middle);
        this.prefix = prefix;
        this.sufix = sufix;
        this.middle = middle;
    }

    public Range getPrefix()
    {
        return prefix;
    }

    public Range getSufix()
    {
        return sufix;
    }

    public Range getMiddle()
    {
        return middle;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(prefix, sufix, middle);
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
        AIContextParts other = (AIContextParts)obj;
        return Objects.equals(prefix, other.prefix) && Objects.equals(sufix, other.sufix)
            && Objects.equals(middle, other.middle);
    }

    @Override
    public String toString()
    {
        return "AIContextParts [prefix=" + prefix + ", sufix=" + sufix + ", middle=" + middle + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
}
