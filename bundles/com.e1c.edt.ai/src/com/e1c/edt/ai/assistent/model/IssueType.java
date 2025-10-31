/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

// {
/**
 * Тип проблемы.
 */
public enum IssueType
{
    /**
     * Неизвестный тип.
     */
    @SerializedName("undefined")
    Undefined(Messages.IssueTypeUndefined, 0),

    /**
     * Идея.
     */
    @SerializedName("idea")
    Idea(Messages.IssueTypeIdea, 1),

    /**
     * Низкая производительность.
     */
    @SerializedName("low_performance")
    Performance(Messages.IssueTypePerformance, 2),

    /**
     * Низкое качество кода.
     */
    @SerializedName("low_code_quality")
    Quality(Messages.IssueTypeQuality, 3),

    /**
     * Ошибка.
     */
    @SerializedName("error")
    Error(Messages.IssueTypeError, 4);
// }
    public final String Title;
    public final int Index;

    IssueType(String title, int index)
    {
        Title = title;
        Index = index;
    }
// {
}
// }
