/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Часть комментария.
 */
public class CommentDescriptionPart
{
    /**
     * Вид части комментария "text"/"link"/"type"/"parameters"/"return"/"field"/"linkWithType"/"unknown".
     */
    public String kind;

    /**
     * Текст комментария, когда kind == "text".
     */
    public String text;

    /**
     * Ссылка, когда kind == "link" или "linkWithType".
     */
    public String link;

    /**
     * Тип, когда kind == "type".
     */
    public CommentType type;

    /**
     * Поле, когда kind == "field".
     */
    public CommentFieldDefinition field;

    /**
     * Параметры, когда kind == "parameters".
     */
    public CommentParameters parameters;

    /**
     * Возвращаемое значение, когда kind == "return".
     */
    @SerializedName("return")
    public CommentReturn returnInfo;

    /**
     * Список типов, когда kind == "linkWithType".
     */
    @SerializedName("link_to_fields")
    public String linkToExtensionFields;

    /**
     * Имя типа, когда kind == "linkWithType".
     */
    @SerializedName("type_name")
    public String typeName;

    /**
     * Список типов, когда kind == "linkWithType".
     */
    @SerializedName("containing_type_definitions")
    public List<CommentTypeDefinition> containingTypeDefinitions;

    /**
     * Список полей, когда kind == "linkWithType".
     */
    @SerializedName("field_definitions")
    public List<CommentFieldDefinition> fieldDefinitions;
}
