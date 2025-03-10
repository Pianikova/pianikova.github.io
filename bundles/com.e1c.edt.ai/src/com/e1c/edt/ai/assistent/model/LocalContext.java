/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class LocalContext
{
    public String prefix;

    public String suffix;

    public String path;

    public Integer offset;

    @SerializedName("optional_response")
    public boolean optionalResponse;

    @SerializedName("script_language")
    public String scriptLanguage;

    @SerializedName("programing_language")
    public String programingLanguage;

    @SerializedName("cursor_object")
    public String cursorObject;

    @SerializedName("current_method")
    public String currenMethodName;

    @SerializedName("cursor_areas")
    public List<String> cursorAreas;

    @SerializedName("cursor_environments")
    public List<String> cursorEnvironments;

    @SerializedName("related_objects")
    public List<Object> relatedObjects;

    @SerializedName("related_functions")
    public List<Object> relatedFunctions;

    @SerializedName("proposals")
    public List<String> proposals;
}
