/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Base request class for execute tools.
 */
public class BaseExecuteRequest
{
    @SerializedName("working_directory")
    public String working_directory;

    @SerializedName("args")
    public List<String> args;

    @SerializedName("timeout")
    public Long timeout;
}
