/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CommentDescriptionPart
{
    public String kind;

    public String text;

    public String link;

    public CommentType type;

    public CommentFieldDefinition field;

    public CommentParameters parameters;

    @SerializedName("return")
    public CommentReturn returnInfo;

    @SerializedName("link_to_fields")
    public String linkToExtensionFields;

    @SerializedName("type_name")
    public String typeName;

    @SerializedName("containing_type_definitions")
    public List<CommentTypeDefinition> containingTypeDefinitions;

    @SerializedName("field_definitions")
    public List<CommentFieldDefinition> fieldDefinitions;
}
