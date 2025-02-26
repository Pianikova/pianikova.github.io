/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class FormTableEntity
    extends FormGroupEntity
{
    @SerializedName("data_path")
    public String dataPath;

    @SerializedName("table_fields")
    public List<FieldEntity> tableFields;
}
