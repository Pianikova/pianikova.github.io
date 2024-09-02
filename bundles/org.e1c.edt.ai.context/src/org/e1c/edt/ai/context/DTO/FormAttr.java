/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class FormAttr
{
    public String name;

    public Map<String, String> title;

    public String type;

    @SerializedName("type_ru")
    public String typeRu;
}
