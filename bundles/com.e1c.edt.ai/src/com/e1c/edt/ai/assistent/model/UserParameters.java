/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.e1c.edt.ai.CodeCompletionPolicy;
import com.google.gson.annotations.SerializedName;

public class UserParameters
{
    @SerializedName("edt_version")
    public String edtVersion;

    @SerializedName("plugin_version")
    public String pluginVersion;

    @SerializedName("tab_width")
    public int tabWidth;

    @SerializedName("code_completion_lines_count")
    public int codeCompletionLinesCount;

    @SerializedName("code_completion_policy")
    public CodeCompletionPolicy codeCompletionPolicy;

    @SerializedName("min_request_delay_ms")
    public long minRequestDelayMs;

    @SerializedName("timeout_ms")
    public long timeoutMs;

    @SerializedName("line_separator")
    public String lineSeparator;

    @SerializedName("send_context")
    public boolean sendContext;

    @SerializedName("language")
    public String language;
}
