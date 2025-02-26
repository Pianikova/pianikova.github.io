/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class GlobalContextUpdateResponse
{
    @SerializedName("unk_vals")
    public List<EntityValue> unknownValues;

    @SerializedName("unk_keys")
    public List<EntityKey> unknownKeys;
}
