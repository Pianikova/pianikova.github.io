/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class AttributeEntity
    extends PropertyEntity
{
    @SerializedName("is_main")
    public Boolean isMain;

    public Map<String, String> title;

    @SerializedName("tool_tip")
    public Map<String, String> toolTip;

    @SerializedName("dynamic_list")
    public DynamicListEntity dynamicList;

    @SerializedName("value_list")
    public ValueListEntity valueList;
}
