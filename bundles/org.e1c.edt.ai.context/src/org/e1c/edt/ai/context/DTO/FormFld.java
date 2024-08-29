/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class FormFld
{
    public String name;

    public String fiedType;

    @SerializedName("tool_tip")
    public Map<String, String> toolTip;

    @SerializedName("data_path")
    public String dataPth;
}
