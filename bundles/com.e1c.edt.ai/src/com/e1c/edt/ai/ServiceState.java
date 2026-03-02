/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

/**
 * @author Bogdan Sushkov
 *
 */
public enum ServiceState
{
    MISSING_TOKEN(false),
    TOKEN_ERROR(false),
    SERVER_ERROR(false),
    SETTINGS_CHANGED(false),
    SSL_ERROR(false),
    SESSION_EXPIRED(true),
    OFFLINE(false),
    ONLINE(false);

    private final boolean allowDuplicates;

    ServiceState(boolean allowDuplicates)
    {
        this.allowDuplicates = allowDuplicates;
    }

    public boolean isAllowDuplicates()
    {
        return allowDuplicates;
    }
}
