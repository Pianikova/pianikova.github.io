/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * DTO class for storing the results of process execution
 */
public class ProcessResult
{
    @SerializedName("exit_code")
    public int exitCode;

    @SerializedName("std_out")
    public String stdOut;

    @SerializedName("std_err")
    public String stdErr;
}
