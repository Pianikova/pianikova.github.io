/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MetaEntity
{
    public List<AttributeEntity> attributes;

    @SerializedName("standard_attributes")
    public List<AttributeEntity> standardAttributes;

    public List<FieldEntity> fields;

    @SerializedName("tabular_sections")
    public List<TabularSectionEntity> tabularSections;

    @SerializedName("register_resources")
    public List<RegisterResourceEntity> registerResources;

    @SerializedName("register_dimensions")
    public List<RegisterDimensionEntity> registerDimensions;

    @SerializedName("register_records")
    public List<RegisterRecordEntity> registerRecords;

    @SerializedName("enum_values")
    public List<EnumValueEntity> enumValues;

    @SerializedName("object_form")
    public List<ObjectFormEntity> objectForms;

    // Движение - проведение
    public String posting;

    // Движение - оперативное проведение
    @SerializedName("real_time_posting")
    public String realTimePosting;

    // Движение - удаление движений
    @SerializedName("register_records_deletion")
    public String registerRecordsDeletion;
}
