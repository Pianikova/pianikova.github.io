/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CommentType
{
    public List<CommentDescriptionPart> description;

    @SerializedName("source_description")
    public List<CommentDescriptionPart> sourceDescription;

    @SerializedName("source_extension_description")
    public List<CommentDescriptionPart> sourceExtensionDescription;

    @SerializedName("type_definitions")
    public List<CommentTypeDefinition> typeDefinitions;
}
