/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Отзыв от пользователя.
 */
public class IssueFeedback
{
    /**
     * Тип проблемы.
     */
    @SerializedName("issue_type")
    public IssueType issueType;

    /**
     * Описание проблемы.
     */
    @SerializedName("issue_description")
    public String issueDescription;

    /**
     * Идентификатор запроса на продолжение кода, связанного с принятым фрагментом кода.
     */
    @SerializedName("request_uuid")
    public String requestUuid;

    /**
     * Полезная информацию об окружении пользователя
     * EDT_version (Строка) - версия EDT
     * plugin_version (Строка) - версия плагина
     */
    @SerializedName("meta_info")
    public Map<String, String> metaInfo;
}
// }
