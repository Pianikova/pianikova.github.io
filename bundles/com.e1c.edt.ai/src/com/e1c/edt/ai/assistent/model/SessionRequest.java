/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Запрос сессии.
 */
public class SessionRequest
{
    /**
     * Параметры сессии.
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

    /**
     * Параметры проекта.
     */
    @SerializedName("project_parameters")
    public ProjectParameters projectParameters;

    /**
     * Параметры рабочего окружения (workspace).
     */
    @SerializedName("workspace_parameters")
    public WorkspaceParameters workspaceParameters;
}
// }
