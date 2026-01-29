package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;

/**
 * Result of reading a stream with content and limitation flag
 */
public class StreamReadResult
{
    @SerializedName("content")
    public String content;

    @SerializedName("truncated")
    public boolean truncated;

    public StreamReadResult(String content, boolean truncated)
    {
        this.content = content;
        this.truncated = truncated;
    }
}