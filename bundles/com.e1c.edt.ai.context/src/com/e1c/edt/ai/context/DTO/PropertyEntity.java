/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class PropertyEntity
{
    public String name;

    @SerializedName("name_ru")
    public String nameRu;

    public String description;

    @SerializedName("data_paths")
    public List<String> dataPaths;

    public List<DataType> types;

    public List<PropertyEntity> properties;
}
