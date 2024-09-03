/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;

import org.e1c.edt.ai.context.Entity;

import com.google.gson.annotations.SerializedName;

public class RelatedEntitiesResponse
{
    @SerializedName("related_objects")
    public List<Entity> relatedObjects;

    @SerializedName("related_functions")
    public List<Entity> relatedFunctions;

    @SerializedName("local_functions")
    public List<MethodEntity> localFunctions;

    public FormEntity form;
}
