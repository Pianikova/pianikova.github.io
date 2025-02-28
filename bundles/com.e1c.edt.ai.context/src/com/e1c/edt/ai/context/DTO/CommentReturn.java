/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CommentReturn
{
    @SerializedName("return_description")
    public List<CommentDescriptionPart> returnDescription;

    @SerializedName("return_types")
    public List<CommentType> returnTypes;
}
