/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Описание праметров метода.
 */
public class CommentParameters
{
    /**
     * Список параметров метода.
     */
    public List<CommentParameter> parameters;

    /**
     * Список полей параметров метода.
     */
    @SerializedName("parameters_field_definitions")
    public List<CommentFieldDefinition> parametersFieldDefinitions;

    /**
     * Описанике параметров.
     */
    @SerializedName("parameters_description")
    public List<CommentDescriptionPart> parametersDescription;

    /**
     * Описание источника.
     */
    @SerializedName("source_description")
    public List<CommentDescriptionPart> sourceDescription;
}
