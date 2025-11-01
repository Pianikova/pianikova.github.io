/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

/**
 * Содержит информацию о вызываемой функции и ее аргументах
 */
public class McpToolCallFunctionCall
{
    /**
     * Название вызываемой функции
     * Пример: "get_weather"
     */
    public String name;

    /**
     * Аргументы вызова функции в виде пар ключ-значение
     * Пример: { "location": "San Francisco, CA" }
     */
    public String arguments;
}
