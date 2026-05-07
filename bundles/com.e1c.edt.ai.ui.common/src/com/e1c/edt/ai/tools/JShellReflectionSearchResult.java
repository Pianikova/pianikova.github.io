/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class JShellReflectionSearchResult
{
    @SerializedName("kind")
    public String kind;

    @SerializedName("fqn")
    public String fqn;

    @SerializedName("items")
    public List<String> items;

    @SerializedName("truncated")
    public boolean truncated;
}
