/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import com.google.gson.annotations.SerializedName;

public class Parameter
{
    public String name;

    public Boolean required;

    public String type;

    @SerializedName("type_ru")
    public String typeRu;
}
