/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

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
