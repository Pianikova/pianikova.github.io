/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * Информация о сессии.
 */
public class Session
{
    /**
     * Идентификатор сессии.
     */
    @SerializedName("session_id")
    public String sessionId;

    /**
     * Параметры сервиса.
     */
    @SerializedName("user_parameters")
    public Parameters userParameters;
}
