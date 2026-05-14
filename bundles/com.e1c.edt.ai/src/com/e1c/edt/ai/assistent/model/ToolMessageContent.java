/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author Bogdan Sushkov
 *
 */
public class ToolMessageContent
{
    @SerializedName("content")
    public String content;

    /**
     * Идентификатор вызова инструмента, который был в ответе на предыдущее сообщение
     */
    @SerializedName("tool_call_id")
    public String toolCallId;

    /**
     * Статус выполнения инструмента (ok | accepted | rejected | error | timeout)
     */
    @SerializedName("status")
    public String status;
}
