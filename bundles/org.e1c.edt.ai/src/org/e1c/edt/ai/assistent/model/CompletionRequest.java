/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class CompletionRequest
{
    @SerializedName("local_context")
    public LocalContext localContext;
}