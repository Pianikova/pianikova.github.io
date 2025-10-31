/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Ответ на запрос обновления локального и глобального контекста.
 */
public class GlobalContextUpdateResponse
{
    /**
     * Список неизвестных значений.
     */
    @SerializedName("unk_vals")
    public List<EntityValue> unknownValues;

    /**
     * Список неизвестных ключей.
     */
    @SerializedName("unk_keys")
    public List<EntityKey> unknownKeys;
// }
    public boolean isEmpty()
    {
        return unknownValues == null || unknownValues.isEmpty();
    }
// {
}
// }
