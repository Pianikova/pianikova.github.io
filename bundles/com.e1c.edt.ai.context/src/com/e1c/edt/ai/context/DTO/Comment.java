/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Comment
{
    public List<CommentDescriptionPart> description;

    public CommentParameters parameters;

    @SerializedName("example_description")
    public List<CommentDescriptionPart> exampleDescription;

    @SerializedName("call_options_description")
    public List<CommentDescriptionPart> callOptionsDescription;

    @SerializedName("return")
    public CommentReturn returnInfo;
}
