/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

public class ObjectEntityField
{
    /**
     * Имя поля.
     */
    public String name;

    /**
     * Типы данных, которые принимает поле в коде.
     */
    public List<DataType> types;
}
