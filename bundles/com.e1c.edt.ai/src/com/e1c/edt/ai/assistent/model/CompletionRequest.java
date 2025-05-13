/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

/**
 * Запрос на продолжение кода.
 */
public class CompletionRequest
{
    /**
     * Оперативный контекст.
     */
    @SerializedName("local_context")
    public LocalContext localContext;
}