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
    @SerializedName("service_parameters")
    public Parameters serviceParameters;
}
