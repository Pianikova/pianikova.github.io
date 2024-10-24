/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class DataType
{
    public String type;

    @SerializedName("type_ru")
    public String typeRu;

    public List<ObjectEntityField> fields;

    public String uuid;

    public List<String> comment;

    @Override
    public int hashCode()
    {
        return Objects.hash(type, typeRu, uuid);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataType other = (DataType)obj;
        return Objects.equals(type, other.type) && Objects.equals(typeRu, other.typeRu)
            && Objects.equals(uuid, other.uuid);
    }
}
