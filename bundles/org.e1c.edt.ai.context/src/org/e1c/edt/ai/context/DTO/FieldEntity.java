/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class FieldEntity
{
    public String name;

    @SerializedName("name_ru")
    public String nameRu;

    public List<DataType> types;
}
