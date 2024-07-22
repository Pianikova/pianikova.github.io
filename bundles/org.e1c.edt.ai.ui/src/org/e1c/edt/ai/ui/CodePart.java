/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.Range;
import org.e1c.edt.ai.assistent.model.CursorLocation;

import com.google.common.base.Preconditions;

public class CodePart
{
    private final Range range;
    private final CursorLocation location;

    public CodePart(Range range, CursorLocation location)
    {
        Preconditions.checkNotNull(range);
        this.range = range;
        this.location = location;
    }

    public Range getRange()
    {
        return range;
    }

    public CursorLocation getLocation()
    {
        return location;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return range + ": " + location;
    }
}
