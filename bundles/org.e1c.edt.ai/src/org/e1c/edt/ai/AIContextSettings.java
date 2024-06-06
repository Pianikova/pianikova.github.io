/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;

public class AIContextSettings
{
    private final int maxLength;
    private final boolean templeted;

    public AIContextSettings(int maxLength, boolean templeted)
    {
        Preconditions.checkArgument(maxLength > 0);
        this.maxLength = maxLength;
        this.templeted = templeted;
    }

    public int getMaxLength()
    {
        return maxLength;
    }

    public boolean isTempleted()
    {
        return templeted;
    }
}
