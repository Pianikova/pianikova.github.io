/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class ConversationAskRequest
{
    /**
     * Идентификатор сообщения на который отвечаем
     */
    @SerializedName("parent_uuid")
    public String parentUuid;

    /**
     * Содержимое запроса
     */
    @SerializedName("tool_content")
    public ConversationRequestContent content;
}
