/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class SignatureStructurized
{
    public String name;

    public List<String> preprocess;

    public List<String> attributes;

    public List<Parameter> parameters;

    @SerializedName("return_type")
    public String returnType;

    @SerializedName("return_type_ru")
    public String returnTypeRu;
}
