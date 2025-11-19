/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Комментарии к типу.
 */
public class CommentTypeDefinition
{
    /**
     * Наименование типа.
     */
    public String name;

    /**
     * Поля типа.
     */
    @SerializedName("field_definitions")
    public List<CommentFieldDefinition> fieldDefinitions;
}
