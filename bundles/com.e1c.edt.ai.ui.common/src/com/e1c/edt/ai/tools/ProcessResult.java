package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;

public class ProcessResult
{
    @SerializedName("exit_code")
    public int exitCode;

    @SerializedName("std_out")
    public String stdOut;

    @SerializedName("std_err")
    public String stdErr;
}