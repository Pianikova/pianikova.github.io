/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

/**
 * Спецификация инструмента вызова функции
 */
public class McpToolCallSpecification
{
    /**
     * Тип инструмента (всегда "function")
     * Пример: "function"
     */
    public String type;

    /**
     * Детальное описание вызываемой функции
     */
    public McpToolCallFunction function;
}
