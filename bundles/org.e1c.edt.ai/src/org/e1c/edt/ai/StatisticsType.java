/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

public enum StatisticsType
{
    TOTAL_DURATUION("X-Total-Duration"), //$NON-NLS-1$
    SERIALIZATION_DURATUION("X-Serialization-Duration"), //$NON-NLS-1$
    COMPRESSION_DURATUION("X-Compression-Duration"), //$NON-NLS-1$
    CONTEXT_DURATUION("X-Context-Duration"), //$NON-NLS-1$
    AI_CONTEXT_DURATUION("X-AI-Context-Duration"), //$NON-NLS-1$
    LOCAL_CONTEXT_DURATUION("X-Local-Context-Duration"), //$NON-NLS-1$
    GLOBAL_CONTEXT_HASHING_DURATUION("X-Global-Context-Hashing-Duration"), //$NON-NLS-1$
    GLOBAL_CONTEXT_DURATUION("X-Global-Context-Duration"), //$NON-NLS-1$
    LOAD_MODULE_DURATUION("X-Load-Module-Duration"), //$NON-NLS-1$
    FORM_DURATUION("X-Form-Duration"), //$NON-NLS-1$
    META_DURATUION("X-Meta-Duration"), //$NON-NLS-1$
    RELATED_OBJECTS_DURATUION("X-Related-Objects-Duration"), //$NON-NLS-1$
    RELATED_FUNCTIONS_DURATUION("X-Related-Functions-Duration"), //$NON-NLS-1$
    LOCAL_FUNCTIONS_DURATUION("X-Local-Functions-Duration"), //$NON-NLS-1$
    UNPROCESSED_ITEMS("X-Unprocessed-Items"); //$NON-NLS-1$

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
