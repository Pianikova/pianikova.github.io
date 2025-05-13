/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

/**
 * Параметры метода
 */
public class Parameter
{
    /**
     * Имя параметра.
     */
    public String name;

    /**
     * True если обязательный.
     */
    public Boolean required;

    /**
     * Возможные типы параметра.
     */
    public List<DataType> types;
}
