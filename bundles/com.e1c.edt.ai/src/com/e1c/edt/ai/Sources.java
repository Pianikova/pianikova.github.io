/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

public class Sources
{
    public static final ISource UNKNOWN = new UnknownSource();

    private static class UnknownSource
        implements ISource
    {
        private CodeMethod method = new CodeMethod("", 0, 0, Optional.empty()); //$NON-NLS-1$

        @Override
        public String getId()
        {
            return ""; //$NON-NLS-1$
        }

        @Override
        public CodeMethod getMethod()
        {
            return method;
        }
    }
}
