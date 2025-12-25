/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CommandDescription
{
    @SerializedName("command_id")
    public String id;

    @SerializedName("command_name")
    public String name;

    @SerializedName("command_description")
    public String description;

    @SerializedName("command_return_is_defined")
    public boolean returnIsDefined;

    @SerializedName("command_return_type_id")
    public String returnTypeId;

    @SerializedName("parameters")
    public List<CommandParameter> parameters;

    @SerializedName("hot_key")
    public String hotKey;
}
