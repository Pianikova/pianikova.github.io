/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class CompletionRequest
{
    @SerializedName("local_context")
    public LocalContext localContext;
}