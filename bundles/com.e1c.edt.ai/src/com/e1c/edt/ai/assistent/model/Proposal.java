/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class Proposal
{
    @SerializedName("display_string")
    public String displayString;

    public int priority;

    public String prefix;

    public String text;

    public String description;
}
