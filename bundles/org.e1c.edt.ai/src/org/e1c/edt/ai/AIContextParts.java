/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class AIContextParts
{
    private Range prefix;
    private Range sufix;

    public AIContextParts(Range prefix, Range sufix)
    {
        Preconditions.checkNotNull(prefix);
        Preconditions.checkNotNull(sufix);
        this.prefix = prefix;
        this.sufix = sufix;
    }

    public Range getPrefix()
    {
        return prefix;
    }

    public Range getSufix()
    {
        return sufix;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(prefix, sufix);
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
        return Objects.equals(prefix, other.prefix) && Objects.equals(sufix, other.sufix);
    }

    @Override
    public String toString()
    {
        return "AIContextParts [prefix=" + prefix + ", sufix=" + sufix + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
