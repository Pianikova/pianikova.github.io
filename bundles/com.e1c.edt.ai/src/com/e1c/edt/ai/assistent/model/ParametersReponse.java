/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * Ответ на запрос параметров сервиса.
 */
public class ParametersReponse
{
    /**
     * Параметры влияющие на настройки сервиса.
     */
    @SerializedName("service_parameters")
    public Parameters serviceParameters;

    /**
     * Параметры влияющие на настройки пользователя.
     */
    @SerializedName("user_parameters")
    public Parameters userParameters;
}
