/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class FieldEntity
    extends ChildEntity
{
    @SerializedName("name_ru")
    public String nameRu;

    public List<DataType> types;
}
