/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class VisualField
{
    public String name;

    public String value;

    @SerializedName("is_focused")
    public Boolean isFocused;

    @SerializedName("is_multiline")
    public Boolean isMultiline;
}
