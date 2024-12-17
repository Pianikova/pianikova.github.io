/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class GlobalContextUpdateResponse
{
    @SerializedName("unk_vals")
    public List<EntityValue> unknownValues;
}
