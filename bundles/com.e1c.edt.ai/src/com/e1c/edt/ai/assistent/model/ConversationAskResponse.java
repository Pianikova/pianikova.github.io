/**
 *
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class ConversationAskResponse
{
    /**
     * Идентификатор сообщения
     */
    @SerializedName("uuid")
    public String uuid;

    /**
     * Источник сообщения ("user" | "assistant" | "tool")
     */
    public String role;

    /**
     * Идентификатор предыдущего сообщения
     */
    @SerializedName("parent_uuid")
    public String parentUuid;

    /**
     * Содержимое ответа
     */
    public AssistantMessageContent content;

    /**
     * Дельта контента (для потоковой передачи)
     */
    @SerializedName("content_delta")
    public AssistantMessageContentDelta contentDelta;

    /**
     * Флаг завершения генерации сообщения
     */
    public Boolean finished;
}
