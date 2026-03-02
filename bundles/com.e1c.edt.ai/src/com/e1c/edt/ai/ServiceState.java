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
    MISSING_TOKEN(false, Messages.StatusMissingToken),
    TOKEN_ERROR(false, Messages.StatusTokenFailed),
    SERVER_ERROR(false, Messages.StatusServerError),
    SETTINGS_CHANGED(false, Messages.StatusSettingsChanged),
    SSL_ERROR(false, Messages.StatusSSLFailed),
    SESSION_EXPIRED(true, Messages.StatusSessionExpired),
    OFFLINE(false, Messages.StatusOffline),
    ONLINE(false, Messages.StatusOnline);

    private final boolean allowDuplicates;
    private final String message;

    ServiceState(boolean allowDuplicates, String message)
    {
        this.allowDuplicates = allowDuplicates;
        this.message = message;
    }

    public boolean isAllowDuplicates()
    {
        return allowDuplicates;
    }

    public String getMessage()
    {
        return message;
    }
}
