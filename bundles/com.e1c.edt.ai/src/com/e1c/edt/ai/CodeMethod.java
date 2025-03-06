/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.xtext.parser.IParseResult;

import com.google.common.base.Preconditions;

public class CodeMethod
{
    private final String uniqueName;
    private final int startOffest;
    private final int endOffest;
    private final Optional<IParseResult> parseResult;

    public CodeMethod(String uniqueName, int startOffest, int endOffest, Optional<IParseResult> parseResult)
    {
        Preconditions.checkNotNull(uniqueName);
        Preconditions.checkArgument(startOffest <= endOffest);
        this.startOffest = startOffest;
        this.endOffest = endOffest;
        this.uniqueName = uniqueName;
        this.parseResult = parseResult;
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

    public Optional<IParseResult> getParseResult()
    {
        return parseResult;
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
