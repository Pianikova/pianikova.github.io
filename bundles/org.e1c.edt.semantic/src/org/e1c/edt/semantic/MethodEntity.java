/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import com.google.gson.annotations.SerializedName;

public class MethodEntity
{
    String uuid;

    String path;

    Integer start;

    Integer finish;

    String code;

    String area;

    @SerializedName("signature_str")
    String signatureStr;

    @SerializedName("signature_structurized")
    SignatureStructurized signatureStructurized;
}
