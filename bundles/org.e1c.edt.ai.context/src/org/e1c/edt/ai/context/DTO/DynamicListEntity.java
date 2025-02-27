/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class DynamicListEntity
{
    public String query;

    public List<String> keyField;

    public String keyTypeName;

    @SerializedName("main_table_name")
    public String mainTableName;

    @SerializedName("main_table_name_ru")
    public String mainTableNameRu;
}
