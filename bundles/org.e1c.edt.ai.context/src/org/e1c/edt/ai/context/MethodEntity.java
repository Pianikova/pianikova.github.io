/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MethodEntity
{
    String uuid;

    String path;

    Integer start;

    Integer finish;

    String name;

    String code;

    String area;

    @SerializedName("signature_str")
    String signatureStr;

    @SerializedName("signature_structurized")
    SignatureStructurized signatureStructurized;

    List<String> comment;

    @SerializedName("сomment_structurized")
    Comment structurizedСomment;
}
