/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.time.LocalDateTime;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Completion
{
    @SerializedName("text")
    public String text;

    @SerializedName("finish_reason")
    public String finishReason;

    @SerializedName("uuid")
    public String uuid;

    @SerializedName("unk_vals")
    public List<EntityValue> unknownValues;

    @SerializedName("unk_keys")
    public List<EntityKey> unknownKeys;

    @SerializedName("used_keys")
    public List<EntityKey> usedKeys;

    public transient LocalDateTime startTime;
}
