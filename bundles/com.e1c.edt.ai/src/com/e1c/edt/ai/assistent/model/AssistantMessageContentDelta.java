/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * @author Bogdan Sushkov
 *
 */
public class AssistantMessageContentDelta
{
    /**
     * Ответ LLM
     */
    @SerializedName("content")
    public String content;

    /**
     * Текст, который LLM генерирует в процессе размышлений
     */
    @SerializedName("reasoning_content")
    public String reasoningContent;

    /**
     * Вызовы инструментов
     */
    @SerializedName("tool_calls")
    public List<ChoiceDeltaToolCall> toolCalls;
}
