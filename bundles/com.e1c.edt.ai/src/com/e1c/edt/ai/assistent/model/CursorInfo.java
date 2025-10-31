/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Информация о курсоре.
 */
public class CursorInfo
{
    /**
     * Позиция курсора.
     */
    @SerializedName("location")
    public CursorLocation location;

    /**
     * Относительная позиция курсора.
     */
    @SerializedName("relative_location")
    public RelativeLocation relativeLocation;
}
// }
