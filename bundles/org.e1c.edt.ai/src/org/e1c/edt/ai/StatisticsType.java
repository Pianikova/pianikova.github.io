/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public enum StatisticsType
{
    TOTAL("X-Total-Duration"), //$NON-NLS-1$
    SERIALIZATION("X-Serialization-Duration"), //$NON-NLS-1$
    COMPRESSION("X-Compression-Duration"), //$NON-NLS-1$
    CONTEXT("X-Context-Duration"), //$NON-NLS-1$
    LOAD_MODULE("X-Load-Module-Duration"), //$NON-NLS-1$
    FORM("X-Form-Duration"), //$NON-NLS-1$
    META("X-Meta-Duration"), //$NON-NLS-1$
    RELATED_OBJECTS("X-Related-Objects-Duration"), //$NON-NLS-1$
    RELATED_FUNCTIONS("X-Related-Functions-Duration"), //$NON-NLS-1$
    LOCAL_FUNCTIONS("X-Local-Functions-Duration"); //$NON-NLS-1$

    private final String header;

    StatisticsType(String header)
    {
        this.header = header;
    }

    public String getHeader()
    {
        return header;
    }
}
