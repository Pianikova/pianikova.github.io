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
    MISSING_TOKEN,
    TOKEN_ERROR,
    SERVER_ERROR,
    SETTINGS_CHANGED,
    SSL_ERROR,
    SESSION_EXPIRED,
    OFFLINE,
    ONLINE;
}
