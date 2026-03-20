/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author Bogdan Sushkov
 *
 */
public class ChoiceDeltaToolCallFunction
{
    /**
     * JSON-строка с аргументами для вызова инструментов.
     * Не всегда является валидным JSON
     */
    @SerializedName("arguments")
    public String arguments;

    /**
     * Имя инструмента
     */
    @SerializedName("name")
    public String name;
}
