/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class SignatureStructurized
{
    public String name;

    public List<String> preprocess;

    public List<String> attributes;

    public List<Parameter> parameters;

    @SerializedName("return_types")
    public List<DataType> returnTypes;
}
