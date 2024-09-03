/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.e1c.edt.ai.Range;
import org.e1c.edt.ai.assistent.model.CursorLocation;

import com.google.common.base.Preconditions;

public class CodePart
{
    private final Range range;
    private final CursorLocation location;
    private final String text;

    public CodePart(Range range, CursorLocation location, String text)
    {
        Preconditions.checkNotNull(range);
        Preconditions.checkNotNull(text);
        this.range = range;
        this.location = location;
        this.text = text;
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

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return range + ": " + location;
    }
}
