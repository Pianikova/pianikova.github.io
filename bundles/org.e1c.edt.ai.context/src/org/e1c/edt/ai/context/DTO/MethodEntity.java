/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MethodEntity
{
    public String uuid;

    public String path;

    public Integer start;

    public Integer finish;

    public String name;

    public String kind;

    public String code;

    public List<String> areas;

    @SerializedName("signature_str")
    public String signatureStr;

    @SerializedName("signature_structurized")
    public SignatureStructurized signatureStructurized;

    public List<String> comment;

    @SerializedName("сomment_structurized")
    public Comment structurizedСomment;
}
