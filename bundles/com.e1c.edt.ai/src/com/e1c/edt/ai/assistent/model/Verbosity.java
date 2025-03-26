/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public enum Verbosity
{
    @SerializedName("error")
    ERROR(0),

    @SerializedName("warning")
    WARNING(1),

    @SerializedName("info")
    INFO(2),

    @SerializedName("trace")
    TRACE(3),

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
