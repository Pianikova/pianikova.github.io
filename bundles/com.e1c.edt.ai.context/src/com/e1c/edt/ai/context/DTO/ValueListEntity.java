/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ValueListEntity
{
    @SerializedName("item_types")
    public List<DataType> itemTypes;
}
