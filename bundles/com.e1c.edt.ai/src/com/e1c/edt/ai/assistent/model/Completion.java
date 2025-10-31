/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.time.LocalDateTime;
import java.util.List;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Ответ на запрос продолжения кода.
 */
public class Completion
{
    /**
     * Текст ответа. Содержиь часть ответа от LLM.
     * Для получения полного ответа необходимо склеить значения из всех пакетов.
     */
    @SerializedName("text")
    public String text;

    /**
     * Причина окончания. Содержит значение только в последнем пакете.
     */
    @SerializedName("finish_reason")
    public String finishReason;

    /**
     * Уникальный идентификатор запроса.
     */
    @SerializedName("uuid")
    public String uuid;

    /**
     * Неизвестные значения глобального контекста.
     */
    @SerializedName("unk_vals")
    public List<EntityValue> unknownValues;

    /**
     * Неизвестные ключи глобального контекста.
     */
    @SerializedName("unk_keys")
    public List<EntityKey> unknownKeys;

    /**
     * Использованные ключи глобального контекста.
     */
    @SerializedName("used_keys")
    public List<EntityKey> usedKeys;
// }
    /**
     * Время начала запроса.
     */
    public transient LocalDateTime startTime;
// {
}
// }
