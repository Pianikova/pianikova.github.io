/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.JsonElement;

/**
 * Определение инструмента для передачи в API
 */
public class ToolDefinition
{
	/**
	 * Название инструмента
	 */
	public String name;

	/**
     * Версия инструмента (для server tools)
     */
    public Integer version;

	/**
     * Описание инструмента (для client tools)
     */
	public String description;

	/**
     * Параметры инструмента
     */
    public JsonElement parameters;
}
