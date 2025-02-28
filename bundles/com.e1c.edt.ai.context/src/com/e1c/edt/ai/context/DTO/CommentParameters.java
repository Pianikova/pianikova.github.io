/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CommentParameters
{
    public List<CommentParameter> parameters;

    @SerializedName("parameters_field_definitions")
    public List<CommentFieldDefinition> parametersFieldDefinitions;

    @SerializedName("parameters_description")
    public List<CommentDescriptionPart> parametersDescription;

    @SerializedName("source_description")
    public List<CommentDescriptionPart> sourceDescription;
}
