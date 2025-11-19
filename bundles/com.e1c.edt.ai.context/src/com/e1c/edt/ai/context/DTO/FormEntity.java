/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.e1c.edt.ai.assistent.model.IContextEntity;

public class FormEntity
    extends FormGroupEntity
    implements IContextEntity
{
    public List<AttributeEntity> attributes;

    public List<FormParameterEntity> parameters;
}
