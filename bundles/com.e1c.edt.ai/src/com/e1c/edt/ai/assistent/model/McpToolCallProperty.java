/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

/**
 * Спецификация отдельного параметра функции
 */
public class McpToolCallProperty
{
    /**
     * Тип данных параметра
     * Пример: "string", "integer", "boolean"
     */
    public String type;

    /**
     * Детальное описание назначения параметра
     * Пример: "The city and state, e.g. San Francisco, CA"
     */
    public String description;
}