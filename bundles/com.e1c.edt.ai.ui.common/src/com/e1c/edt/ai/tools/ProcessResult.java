package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;

public class ProcessResult
{
    @SerializedName("exit_code")
    public int exitCode;

    @SerializedName("std_out_truncated")
    public boolean stdOutTruncated;

    @SerializedName("std_err_truncated")
    public boolean stdErrTruncated;

    // Large fields last so they are the first to be dropped if the response is truncated.
    @SerializedName("std_out")
    public String stdOut;

    @SerializedName("std_err")
    public String stdErr;
}