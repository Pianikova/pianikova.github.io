/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Позиция курсора в редакторе.
 */
public enum CursorLocation
{
    /**
     * Внутри комментария.
     */
    @SerializedName("comment")
    Comment,

    /**
     * Внутри метода.
     */
    @SerializedName("outside_function")
    OutsideFunction,

    /**
     * Внутри названия метода.
     */
    @SerializedName("function_name")
    FunctionName,

    /**
     * Внутри аргументов метода.
     */
    @SerializedName("function_arguments")
    FunctionArguments,

    /**
     * Внутри тела метода.
     */
    @SerializedName("function_body")
    FunctionBody
}
// }
