/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.e1c.edt.ai.Range;
import org.e1c.edt.ai.assistent.model.CursorLocation;

import com.google.common.base.Preconditions;

public class CodePart
{
    private final Integer methodId;
    private final Range range;
    private final CursorLocation location;
    private final String text;

    public CodePart(Integer methodId, Range range, CursorLocation location, String text)
    {
        Preconditions.checkNotNull(range);
        Preconditions.checkNotNull(text);
        this.methodId = methodId;
        this.range = range;
        this.location = location;
        this.text = text;
    }

    public Integer getMethodId()
    {
        return methodId;
    }

    public Range getRange()
    {
        return range;
    }

    public CursorLocation getLocation()
    {
        return location;
    }

    public String getText()
    {
        return text;
    }

    @Override
    public String toString()
    {
        return range + ": " + location; //$NON-NLS-1$
    }
}
