/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

public class PredefinedEntity
{
    public String name;

    public List<DataType> types;

    public String description;

    public ValueEntity value;

    public List<PredefinedEntity> predefined;

    public List<PredefinedEntity> displaced;

    public List<PredefinedEntity> child;
}
