/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

public class RelatedEntitiesResponse
{
    @SerializedName("related_objects")
    public ArrayList<Entity> relatedObjects;

    @SerializedName("related_functions")
    public ArrayList<Entity> relatedFunctions;
}
