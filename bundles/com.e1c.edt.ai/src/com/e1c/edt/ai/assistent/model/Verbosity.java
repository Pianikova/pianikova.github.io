/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * Уровень детализации журнала.
 */
public enum Verbosity
{
    /**
     * Выводить только ошибки.
     */
    @SerializedName("error")
    ERROR(0),

    /**
     * Выводить предупреждения.
     */
    @SerializedName("warning")
    WARNING(1),

    /**
     * Выводить информацию.
     */
    @SerializedName("info")
    INFO(2),

    /**
     * Выводить трассировку.
     */
    @SerializedName("trace")
    TRACE(3),

    /**
     * Выводить отладочную информацию.
     */
    @SerializedName("debug")
    DEBUG(4);

    private final int level;

    Verbosity(int level)
    {
        this.level = level;
    }

    public int getLevel()
    {
        return level;
    }
}
