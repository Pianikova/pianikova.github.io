package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class ReadFileContentResponse
{
    /**
     * File contents.
     */
    @SerializedName("contents")
    public String contents;

    /**
     * File encoding, for example, "UTF-8", "windows-1251", "KOI8-R", "UTF-16", "UTF-32", etc.
     */
    @SerializedName("charset_name")
    public String charsetName;
}
