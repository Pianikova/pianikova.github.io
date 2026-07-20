/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * Параметры проекта. Передаются при создании сессии, чтобы сервер мог привязать
 * долговременную память и диалоги к проекту по паре (uuid, token_id).
 */
public class ProjectParameters
{
    /**
     * Максимальная длина uuid — ограничение хранилища на сервере (uuid VARCHAR(70)).
     */
    public static final int MAX_UUID_LENGTH = 70;

    /**
     * Клиентский идентификатор проекта.
     */
    @SerializedName("uuid")
    public String uuid;

    /**
     * Версия платформы, например 8.3.24.
     */
    @SerializedName("platform_version")
    public String platformVersion;

    /**
     * Режим совместимости.
     */
    @SerializedName("compatibilityMode")
    public String compatibilityMode;

    /**
     * Имя конфигурации.
     */
    @SerializedName("configuration_name")
    public String configurationName;

    /**
     * Версия конфигурации, например 1.2.3.
     */
    @SerializedName("configuration_version")
    public String configurationVersion;

    /**
     * Обрезает uuid до {@link #MAX_UUID_LENGTH} символов, если он длиннее.
     *
     * @param uuid исходный идентификатор, может быть {@code null}
     * @return идентификатор длиной не более {@link #MAX_UUID_LENGTH} символов
     */
    public static String limitUuid(String uuid)
    {
        if (uuid == null || uuid.length() <= MAX_UUID_LENGTH)
        {
            return uuid;
        }

        return uuid.substring(0, MAX_UUID_LENGTH);
    }
}
