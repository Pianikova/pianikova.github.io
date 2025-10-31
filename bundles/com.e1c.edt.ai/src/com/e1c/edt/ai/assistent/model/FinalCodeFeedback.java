/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Информация о финальном фрагменте кода.
 */
public class FinalCodeFeedback
{
    /**
     * Финальный фрагмент кода.
     */
    @SerializedName("final_code")
    public String finalCode;

    /**
     * Идентификатор запроса на продолжение кода, связанного с фмнальным фрагментом кода.
     */
    @SerializedName("request_uuid")
    public String requestUuid;
}
// }
