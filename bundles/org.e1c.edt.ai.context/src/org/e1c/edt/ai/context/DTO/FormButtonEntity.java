/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class FormButtonEntity
{
    public String name;

    public Map<String, String> title;

    @SerializedName("data_path")
    public String dataPath;
}
