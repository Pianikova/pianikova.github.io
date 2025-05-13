/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * Запрос сессии.
 */
public class SessionRequest
{
    /**
     * Параметры запроса для запроса к модели.
     */
    @SerializedName("service_parameters")
    public Parameters serviceParameters;

    /**
     * Параметры пользователя.
     */
    @SerializedName("user_parameters")
    public UserParameters userParameters;

    /**
     * Информация о системе пользователя.
     */
    @SerializedName("system_info")
    public SystemInfo systemInfo;
}
