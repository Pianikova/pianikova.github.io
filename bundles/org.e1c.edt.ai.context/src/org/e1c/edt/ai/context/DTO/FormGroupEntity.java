/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class FormGroupEntity
{
    public String name;

    public String kind;

    public Map<String, String> title;

    @SerializedName("tool_tip")
    public Map<String, String> toolTip;

    public List<FormFieldEntity> fields;

    public List<FormGroupEntity> groups;

    public List<FormButtonEntity> buttons;
}
