/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CommentReturn
{
    @SerializedName("return_description")
    public List<CommentDescriptionPart> returnDescription;

    @SerializedName("return_types")
    public List<CommentType> returnTypes;
}
