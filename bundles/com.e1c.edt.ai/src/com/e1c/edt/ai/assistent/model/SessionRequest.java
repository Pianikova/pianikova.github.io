/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class SessionRequest
{
    @SerializedName("service_parameters")
    public Parameters serviceParameters;

    @SerializedName("user_parameters")
    public UserParameters userParameters;

    @SerializedName("system_info")
    public SystemInfo systemInfo;
}
