/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

/**
 * Определение функции для вызова
 */
public class McpToolCallFunction
{
    /**
     * Уникальное имя функции для вызова
     * Пример: "get_weather"
     */
    public String name;

    /**
     * Описание назначения и функциональности
     * Пример: "Get weather of an location, the user should supply a location first"
     */
    public String description;

    /**
     * Параметры, необходимые для выполнения функции
     */
    public McpToolCallParameters parameters;
}