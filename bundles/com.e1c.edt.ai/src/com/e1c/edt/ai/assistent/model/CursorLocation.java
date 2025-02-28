/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public enum CursorLocation
{
    @SerializedName("comment")
    Comment,

    @SerializedName("outside_function")
    OutsideFunction,

    @SerializedName("function_name")
    FunctionName,

    @SerializedName("function_arguments")
    FunctionArguments,

    @SerializedName("function_body")
    FunctionBody
}
