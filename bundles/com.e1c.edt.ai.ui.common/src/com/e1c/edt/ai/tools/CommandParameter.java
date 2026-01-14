/**
 *
 */
package com.e1c.edt.ai.tools;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class CommandParameter
{
    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("value")
    public String value;

    @SerializedName("is_optional")
    public boolean isOptional;

    @SerializedName("values")
    public Map<Object, Object> values;
}
