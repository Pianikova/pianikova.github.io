/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class Completion
{
    @SerializedName("text")
    public String text;

    @SerializedName("finish_reason")
    public String finishReason;

    @SerializedName("uuid")
    public String uuid;
}
