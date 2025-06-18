/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class RelatedEntitiesResponse
{
    public String code;

    @SerializedName("related_objects")
    public List<Entity> relatedObjects;

    @SerializedName("related_functions")
    public List<Entity> relatedFunctions;

    @SerializedName("local_functions")
    public List<MethodEntity> localFunctions;

    public FormEntity form;

    public List<MetaEntity> meta;
}
