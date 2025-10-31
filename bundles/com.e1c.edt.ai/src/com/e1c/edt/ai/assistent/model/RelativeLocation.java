/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Относительная позиция.
 */
public enum RelativeLocation
{
    /**
     * Ближе к началу.
     */
    @SerializedName("start")
    Start,

    /**
     * Ближе к середине.
     */
    @SerializedName("middle")
    Middle,

    /**
     * Ближе к концу.
     */
    @SerializedName("end")
    End
}
// }
