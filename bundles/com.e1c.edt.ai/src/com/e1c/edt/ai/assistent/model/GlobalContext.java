/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class GlobalContext
{
    // initial seeesion conttext
    public transient String configurationName;

    // hash
    @SerializedName("form")
    public String formHash;

    public transient String formPath;

    // hash
    @SerializedName("meta")
    public String metaHash;

    public transient String metaPath;

    // hash
    @SerializedName("module")
    public String moduleHash;

    public transient String modulePath;

    // hashes
    @SerializedName("local_functions")
    public Map<String, String> localFunctions;

    public transient Map<String, HashedValue<Object>> localFunctionsEntities;

    public transient Object formEntity;

    public transient Object metaEntity;
}
