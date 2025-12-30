package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class ReadFileContentResponse
{
    /**
     * File content.
     */
    public String content;

    /**
     * File encoding.
     */
    @SerializedName("charset_name")
    public String charsetName;
}
