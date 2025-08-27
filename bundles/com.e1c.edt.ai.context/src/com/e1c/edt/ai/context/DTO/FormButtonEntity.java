/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class FormButtonEntity
    extends ChildEntity
{
    public Map<String, String> title;

    @SerializedName("data_path")
    public String dataPath;
}
