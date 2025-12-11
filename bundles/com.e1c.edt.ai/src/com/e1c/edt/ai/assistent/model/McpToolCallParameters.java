/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;
import java.util.Map;

/**
 * Параметры вызова функции
 */
public class McpToolCallParameters
{
    /**
     * Тип структуры параметров (всегда "object")
     * Пример: "object"
     */
    public String type;

    /**
     * Коллекция свойств параметров
     * Ключ: имя параметра (например "location")
     * Значение: спецификация параметра
     */
    public Map<String, McpToolCallProperty> properties;

    /**
     * Список обязательных параметров
     * Пример: ["location"]
     */
    public List<String> required;
}