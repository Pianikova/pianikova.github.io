/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class ChatContext
{
    @SerializedName("script_language")
    public String scriptLanguage;

    @SerializedName("programing_language")
    public String programingLanguage;
}
