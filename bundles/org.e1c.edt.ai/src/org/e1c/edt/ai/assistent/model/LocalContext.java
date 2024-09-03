/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class LocalContext
{
    public String prefix;

    public String suffix;

    public String path;

    @SerializedName("related_objects")
    public List<Object> relatedObjects;

    @SerializedName("related_functions")
    public List<Object> relatedFunctions;

    @SerializedName("local_functions")
    public List<Object> localFunctions;

    public Object form;
}
