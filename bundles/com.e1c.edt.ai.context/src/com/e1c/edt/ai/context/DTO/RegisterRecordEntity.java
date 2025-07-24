/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;
import java.util.Map;

public class RegisterRecordEntity
{
    public String name;

    public String comment;

    public Map<String, String> synonym;

    // Too much info:
    // public List<FieldEntity> fields;

    public List<DataType> types;
}
