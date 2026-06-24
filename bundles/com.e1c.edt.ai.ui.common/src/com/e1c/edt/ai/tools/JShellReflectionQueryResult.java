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

    @SerializedName("truncated")
    public boolean truncated;

    // Large fields last so they are dropped first if the response is truncated.
    @SerializedName("results")
    public List<JShellReflectionSearchResult> results;

    @SerializedName("suggestions")
    public List<String> suggestions;
}
