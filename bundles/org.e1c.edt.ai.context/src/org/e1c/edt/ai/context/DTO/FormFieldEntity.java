/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class FormFieldEntity
{
    public String name;

    @SerializedName("field_type")
    public String fieldType;

    @SerializedName("tool_tip")
    public Map<String, String> toolTip;

    @SerializedName("data_path")
    public String dataPath;
}
