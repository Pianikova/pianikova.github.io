/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class JShellReflectionQueryResult
{
    @SerializedName("query")
    public String query;

    @SerializedName("kind")
    public String kind;

    @SerializedName("results")
    public List<JShellReflectionSearchResult> results;

    @SerializedName("truncated")
    public boolean truncated;

    @SerializedName("suggestions")
    public List<String> suggestions;
}
