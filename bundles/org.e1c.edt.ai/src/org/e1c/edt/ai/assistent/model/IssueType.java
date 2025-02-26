/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public enum IssueType
{
    @SerializedName("undefined")
    Undefined(Messages.IssueTypeUndefined, 0),

    @SerializedName("idea")
    Idea(Messages.IssueTypeIdea, 1),

    @SerializedName("low_performance")
    Performance(Messages.IssueTypePerformance, 2),

    @SerializedName("low_code_quality")
    Quality(Messages.IssueTypeQuality, 3),

    @SerializedName("error")
    Error(Messages.IssueTypeError, 4);

    public final String Title;
    public final int Index;

    IssueType(String title, int index)
    {
        Title = title;
        Index = index;
    }
}
