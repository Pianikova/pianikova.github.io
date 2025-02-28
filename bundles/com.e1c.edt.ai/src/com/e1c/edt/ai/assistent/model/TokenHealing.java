/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public enum TokenHealing
{
    @SerializedName("None")
    NONE,

    @SerializedName("guidance")
    GUIDANCE,

    @SerializedName("streaming")
    STREAMING
}
