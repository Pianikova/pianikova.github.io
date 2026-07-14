/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Optional;

import com.google.gson.annotations.SerializedName;

/**
 * @author Bogdan Sushkov
 *
 */
public class ToolMessageContent
{
    /**
     * Результат инструмента. {@code Optional.empty()} сериализуется как явный {@code null} —
     * это обязательно для серверных инструментов со статусом {@code accepted}: сервер требует
     * присутствия поля {@code content} и пустого значения в нём.
     */
    @SerializedName("content")
    public Optional<String> content;

    /**
     * Идентификатор вызова инструмента, который был в ответе на предыдущее сообщение
     */
    @SerializedName("tool_call_id")
    public String toolCallId;

    /**
     * Статус выполнения инструмента. Для клиентских инструментов: ok | rejected | ignored |
     * error | timeout. Для серверных (тип system/mcp на сервере): accepted | rejected | ignored —
     * при accepted сервер выполняет инструмент сам.
     */
    @SerializedName("status")
    public String status;
}
