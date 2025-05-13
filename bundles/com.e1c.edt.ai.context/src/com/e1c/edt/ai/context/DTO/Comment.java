/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Структурированный комментарий метода.
 */
public class Comment
{
    /**
     * Части комментария.
     */
    public List<CommentDescriptionPart> description;

    /**
     * Описание праметров метода.
     */
    public CommentParameters parameters;

    /**
     * Комментарии секции "примеры".
     */
    @SerializedName("example_description")
    public List<CommentDescriptionPart> exampleDescription;

    /**
     * Комментарии секции "опции вызова".
     */
    @SerializedName("call_options_description")
    public List<CommentDescriptionPart> callOptionsDescription;

    /**
     * Комментарии секции "возвращаемое значение".
     */
    @SerializedName("return")
    public CommentReturn returnInfo;
}
