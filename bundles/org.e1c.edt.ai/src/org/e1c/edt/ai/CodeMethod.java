/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class CodeMethod
{
    private final String uniqueName;
    private final int startOffest;
    private final int endOffest;

    public CodeMethod(String uniqueName, int startOffest, int endOffest)
    {
        this.startOffest = startOffest;
        this.endOffest = endOffest;
        Preconditions.checkNotNull(uniqueName);
        this.uniqueName = uniqueName;
    }

    public String getUniqueName()
    {
        return uniqueName;
    }

    public int getStartOffest()
    {
        return startOffest;
    }

    public int getEndOffest()
    {
        return endOffest;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(uniqueName);
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
        CodeMethod other = (CodeMethod)obj;
        return Objects.equals(uniqueName, other.uniqueName);
    }
}
