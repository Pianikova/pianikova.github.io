/**
 *
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;

/**
 * Сообщение от инструмента (tool) в формате OpenAI
 */
public class ToolCallMessage
{
    /**
     * Роль отправителя сообщения (всегда "tool")
     * Пример: "tool"
     */
    public String role;

    /**
     * Содержимое сообщения - результат выполнения функции
     * Пример: "24℃ sunny"
     */
    public String content;

    /**
     * Идентификатор вызова инструмента, на который отвечает это сообщение
     * Должен соответствовать ToolCall.id из запроса
     * Пример: "call_eeb3ed1c979341c29c0ec637"
     */
    public String tool_call_id;

    /**
     * Детали вызова инструмента, на который отвечает это сообщение
     */
    public ToolCallMessageDetails details;

    /**
     * Спецификация вызова инструмента, на который отвечает это сообщение
     */
    public transient McpToolCallSpecification specification;

    /**
     * Вызов инструмента, на который отвечает это сообщение
     */
    public transient McpToolCall call;
}