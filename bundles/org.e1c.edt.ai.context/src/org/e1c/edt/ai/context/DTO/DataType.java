/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class DataType
{
    public String type;

    @SerializedName("type_ru")
    public String typeRu;

    public List<ObjectEntityField> fields;

    public String uuid;

    public List<String> comment;
}
