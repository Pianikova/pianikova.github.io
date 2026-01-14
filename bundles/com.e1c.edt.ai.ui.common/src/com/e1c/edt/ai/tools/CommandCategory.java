/**
 *
 */
package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;

public class CommandCategory
{
    @SerializedName("category_id")
    public String id;

    @SerializedName("category_name")
    public String name;

    @SerializedName("category_description")
    public String description;
}
