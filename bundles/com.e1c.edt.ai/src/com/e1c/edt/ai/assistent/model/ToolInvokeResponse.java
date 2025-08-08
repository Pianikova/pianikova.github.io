/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class ToolInvokeResponse
{
    /**
     * Идентификатор сообщения
     */
    @SerializedName("uuid")
    public String uuid;

    /**
     * Содержимое ответа
     */
    public ToolInvokeResponseContent content;

    /**
     * Флаг завершения генерации сообщения
     */
    public boolean finished;
}
