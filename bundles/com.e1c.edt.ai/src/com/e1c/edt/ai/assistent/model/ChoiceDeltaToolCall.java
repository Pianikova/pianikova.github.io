/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author Bogdan Sushkov
 *
 */
public class ChoiceDeltaToolCall
{
    /**
     * Индекс инструмента
     */
    @SerializedName("index")
    public int index;

    /**
     * Идентификатор вызова инструмента
     */
    @SerializedName("id")
    public String id;

    /**
     * Вызванный инструмент
     */
    @SerializedName("function")
    public ChoiceDeltaToolCallFunction function;
}
