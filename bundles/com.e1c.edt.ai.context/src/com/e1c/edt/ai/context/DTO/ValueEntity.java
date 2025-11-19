/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import com.e1c.edt.ai.assistent.model.IContextEntity;

public class ValueEntity implements IContextEntity
{
    /**
     * Идентификатор сущности.
     */
    public String id;

    public ValueType type;

    public Object value;
}
