/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class CursorInfo
{
    @SerializedName("location")
    public CursorLocation location;

    @SerializedName("relative_location")
    public RelativeLocation relativeLocation;
}
