/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Структурированная сигнатура метода
 */
public class SignatureStructurized
{
    /**
     * Имя метода.
     */
    public String name;

    /**
     * Директивы препроцессора. Например, [ "НаСервере" ]
     */
    public List<String> preprocess;

    /**
     * Атрибуты метода.
     */
    public List<String> attributes;

    /**
     * Параметры метода.
     */
    public List<Parameter> parameters;

    /**
     * Типы возвращаемых значений.
     */
    @SerializedName("return_types")
    public List<DataType> returnTypes;
}
