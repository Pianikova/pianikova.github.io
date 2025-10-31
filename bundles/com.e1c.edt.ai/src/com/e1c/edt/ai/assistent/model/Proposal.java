/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * Предложение от контекстного ассистента.
 */
public class Proposal
{
    /**
     * Тот как контекстный ассистент отобразил это предложение.
     */
    @SerializedName("display_string")
    public String displayString;

    /**
     * Приоритет предложения в списке контекстного ассистента.
     */
    public int priority;

    /**
     * Префикс предложения. Определяется пользователем.
     */
    public String prefix;

    /**
     * Текст предложения, за исключением префикса.
     */
    public String text;

    /**
     * Описание предложения.
     */
    public String description;
}
