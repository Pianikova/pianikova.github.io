/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class FormGrp
{
    public String name;

    public Map<String, String> title;

    public List<FormFld> fields;

    public List<FormGrp> groups;

    @SerializedName("tool_tip")
    public Map<String, String> toolTip;

    public List<FormBtn> buttons;
}
