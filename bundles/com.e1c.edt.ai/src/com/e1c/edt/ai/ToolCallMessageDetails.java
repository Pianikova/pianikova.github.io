/**
 *
 */
package com.e1c.edt.ai;

import com.google.gson.annotations.SerializedName;

public class ToolCallMessageDetails
{
    /**
     * Автоматически вызвать инструмент
     */
    @SerializedName("auto_call")
    public Boolean autoCall;

    /**
     * Скрыть вызов инструмента
     */
    @SerializedName("hidden")
    public Boolean hidden;

    /**
     * Представление запроса  в формате MARKDOWN, который будет выполнен
     */
    @SerializedName("request_markdown")
    public String requestMarkdown;

    /**
     * Представление ответа  в формате MARKDOWN, который будет возвращен
     */
    @SerializedName("response_markdown")
    public String responseMarkdown;
}
