/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class ToolFeedbackFinalTextRequest
{
    /**
     * Идентификатор сообщения
     */
    @SerializedName("tool_uuid")
    public String uuid;

    /**
     * Итоговый вариант текста
     */
    @SerializedName("final_text")
    public String finalText;
}
