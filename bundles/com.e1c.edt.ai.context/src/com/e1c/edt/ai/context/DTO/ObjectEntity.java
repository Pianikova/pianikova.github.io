/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.e1c.edt.ai.assistent.model.IContextEntity;

/**
 * Объект контекста.
 */
public class ObjectEntity
    implements IContextEntity
{
    /**
     * Наименование объекта.
     */
    public String name;

    /**
     * Типы, которые принимает объект в коде.
     */
    public List<DataType> types;

    /**
     * Поля объекта.
     */
    public List<ObjectEntityField> fields;

    /**
     * Начало использование в коде.
     */
    public Integer start;

    /**
     * Конец использования в коде.
     */
    public Integer finish;

    /**
     * Фрагмент кода.
     */
    public String code;

    /**
     * Комментарий.
     */
    public List<String> comment;
}
