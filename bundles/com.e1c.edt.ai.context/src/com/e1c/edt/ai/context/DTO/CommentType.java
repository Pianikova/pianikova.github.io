/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Комментарии к типу.
 */
public class CommentType
{
    /**
     * Части комментария.
     */
    public List<CommentDescriptionPart> description;

    /**
     * Описание источников типа.
     */
    @SerializedName("source_description")
    public List<CommentDescriptionPart> sourceDescription;

    /**
     * Описание расширений типа.
     */
    @SerializedName("source_extension_description")
    public List<CommentDescriptionPart> sourceExtensionDescription;

    /**
     * Cписок описаний типов.
     */
    @SerializedName("type_definitions")
    public List<CommentTypeDefinition> typeDefinitions;
}
