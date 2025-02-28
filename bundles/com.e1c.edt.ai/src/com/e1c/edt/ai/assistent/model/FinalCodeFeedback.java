/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class FinalCodeFeedback
{
    @SerializedName("final_code")
    public String finalCode;

    @SerializedName("request_uuid")
    public String requestUuid;
}
