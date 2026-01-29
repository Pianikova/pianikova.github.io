/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

/**
 * Представляет вызов инструмента (функции) в формате OpenAI
 */
public class McpToolCall
{
    /**
     * Уникальный идентификатор вызова функции
     * Пример: "call_eeb3ed1c979341c29c0ec637"
     */
    public String id;

    /**
     * Тип вызова (всегда "function")
     * Пример: "function"
     */
    public String type;

    /**
     * Детали вызываемой функции
     */
    public McpToolCallFunctionCall function;

    public transient String sourceChatId;

    public transient String sourceMessageId;

    public transient ToolCallKind callKind;
}