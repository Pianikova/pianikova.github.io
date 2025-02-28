/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class TabularSectionEntity
{
    public String name;

    public String comment;

    public List<FieldEntity> fields;

    @SerializedName("tool_tip")
    public Map<String, String> toolTip;
}
