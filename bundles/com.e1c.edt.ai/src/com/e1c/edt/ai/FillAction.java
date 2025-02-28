/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

public class FillAction
{
    private final DataType dataType;
    private final String field;
    private final String hash;

    public FillAction(DataType dataType, String field, String hash)
    {
        this.dataType = dataType;
        this.field = field;
        this.hash = hash;
    }

    public DataType getDataType()
    {
        return dataType;
    }

    public String getField()
    {
        return field;
    }

    public String getHash()
    {
        return hash;
    }
}
