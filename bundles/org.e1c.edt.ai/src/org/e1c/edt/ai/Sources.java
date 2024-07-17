/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public class Sources
{
    public static final ISource UNKNOWN = new UnknownSource();

    private static class UnknownSource
        implements ISource
    {
        @Override
        public String getId()
        {
            return ""; //$NON-NLS-1$
        }
    }
}
