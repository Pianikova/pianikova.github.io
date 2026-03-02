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
    MISSING_TOKEN(false, Messages.StatusMissingToken, ""), //$NON-NLS-1$
    TOKEN_ERROR(false, Messages.StatusTokenFailed, "troubleshooting/#issue_missing_token"), //$NON-NLS-1$
    SERVER_ERROR(false, Messages.StatusServerError, "troubleshooting/#issue_server_error"), //$NON-NLS-1$
    SETTINGS_CHANGED(false, Messages.StatusSettingsChanged, ""), //$NON-NLS-1$
    SSL_ERROR(false, Messages.StatusSSLFailed, "troubleshooting/#issue_ssl_error"), //$NON-NLS-1$
    SESSION_EXPIRED(true, Messages.StatusSessionExpired, "troubleshooting/#issue_session_expired"), //$NON-NLS-1$
    OFFLINE(false, Messages.StatusOffline, ""), //$NON-NLS-1$
    ONLINE(false, Messages.StatusOnline, ""); //$NON-NLS-1$

    private final boolean allowDuplicates;
    private final String message;
    private final String urlPath;

    ServiceState(boolean allowDuplicates, String message, String urlPath)
    {
        this.allowDuplicates = allowDuplicates;
        this.message = message;
        this.urlPath = urlPath;
    }

    public boolean isAllowDuplicates()
    {
        return allowDuplicates;
    }

    public String getMessage()
    {
        return message;
    }

    public String getUrlPath()
    {
        return urlPath;
    }
}
