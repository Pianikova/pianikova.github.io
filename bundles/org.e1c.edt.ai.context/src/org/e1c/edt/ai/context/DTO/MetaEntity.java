/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.security.MessageDigest;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MetaEntity
{
    public transient MessageDigest hash;

    public List<AttributeEntity> attributes;

    @SerializedName("tabular_sections")
    public List<TabularSectionEntity> tabularSections;

    @SerializedName("register_resources")
    public List<RegisterResourceEntity> registerResources;

    @SerializedName("register_dimensions")
    public List<RegisterDimensionEntity> registerDimensions;

    @SerializedName("register_records")
    public List<RegisterRecordEntity> registerRecords;
}
