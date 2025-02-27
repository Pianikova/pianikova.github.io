/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CommentTypeDefinition
{
    public String name;

    @SerializedName("field_definitions")
    public List<CommentFieldDefinition> fieldDefinitions;
}
