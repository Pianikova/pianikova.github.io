/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public enum Verbosity
{
    @SerializedName("default")
    DEFAULT(0),
    
    @SerializedName("detailed")
    DETAILED(1);

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
