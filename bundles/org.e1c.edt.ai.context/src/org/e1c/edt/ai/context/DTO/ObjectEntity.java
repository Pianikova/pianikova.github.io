/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;

public class ObjectEntity
{
    public String name;

    public List<DataType> types;

    public List<ObjectEntityField> fields;

    public Integer start;

    public Integer finish;

    public String code;

    public List<String> comment;
}
