/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class GlobalContext
{
    public transient String configurationName;

    // hash
    public String form;

    // hash
    public String meta;

    // hashes
    @SerializedName("local_functions")
    public Map<String, String> localFunctions;

    public transient Map<String, HashedValue<Object>> localFunctionsEntities;

    public transient Object formEntity;

    public transient Object metaEntity;
}
