/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class GlobalContext
{
    public transient String configurationName;

    // hash
    public String form;

    public transient String formPath;

    // hash
    public String meta;

    public transient String metaPath;

    // hash
    public String module;

    // hashes
    @SerializedName("local_functions")
    public Map<String, String> localFunctions;

    public transient Map<String, HashedValue<Object>> localFunctionsEntities;

    public transient Object formEntity;

    public transient Object metaEntity;
}
