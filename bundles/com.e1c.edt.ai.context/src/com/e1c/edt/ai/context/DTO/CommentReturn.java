/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Комментарий секции "возвращаемое значение".
 */
public class CommentReturn
{
    /**
     * Описание возвращаемого значения.
     */
    @SerializedName("return_description")
    public List<CommentDescriptionPart> returnDescription;

    /**
     * Описание типов возвращаемых значения.
     */
    @SerializedName("return_types")
    public List<CommentType> returnTypes;
}
