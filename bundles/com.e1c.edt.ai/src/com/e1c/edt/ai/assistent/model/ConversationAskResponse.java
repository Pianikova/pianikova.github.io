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
     * Источник сообщения.
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
    public ConversationResponseContent content;

    /**
     * Время создания сообщения
     */
    @SerializedName("create_time")
    public String createTime;

    /**
     * Флаг завершения генерации сообщения
     */
    public boolean finished;
}
