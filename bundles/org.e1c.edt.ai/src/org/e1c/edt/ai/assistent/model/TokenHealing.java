/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

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
