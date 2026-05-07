/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

class JShellResolvedType
{
    private final String fqn;
    private final Class<?> type;

    JShellResolvedType(String fqn, Class<?> type)
    {
        this.fqn = fqn;
        this.type = type;
    }

    String getFqn()
    {
        return fqn;
    }

    Class<?> getType()
    {
        return type;
    }

    String getSimpleName()
    {
        return type.getSimpleName();
    }
}
