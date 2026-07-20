/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * Параметры рабочего окружения (workspace). Передаются при создании сессии и помогают
 * серверу эффективнее синхронизировать и дедуплицировать глобальный контекст.
 */
public class WorkspaceParameters
{
    /**
     * Клиентский идентификатор рабочего окружения.
     */
    @SerializedName("uuid")
    public String uuid;

    /**
     * Путь к корню рабочего окружения.
     */
    @SerializedName("path")
    public String path;
}
