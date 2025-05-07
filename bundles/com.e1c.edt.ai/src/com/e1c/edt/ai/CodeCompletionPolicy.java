/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.google.gson.annotations.SerializedName;

public enum CodeCompletionPolicy
{
    @SerializedName(CodeCompletionPolicy.OFF_ID)
    OFF(0, CodeCompletionPolicy.OFF_ID, Messages.CodeCompletionPolicy_Off,
        Messages.CodeCompletionPolicy_OffDescription),

    @SerializedName(CodeCompletionPolicy.FOCUSING_ID)
    FOCUSING(1, CodeCompletionPolicy.FOCUSING_ID, Messages.CodeCompletionPolicy_Focusing,
        Messages.CodeCompletionPolicy_FocusingDescription),

    @SerializedName(CodeCompletionPolicy.BALANCE_ID)
    BALANCE(2, CodeCompletionPolicy.BALANCE_ID, Messages.CodeCompletionPolicy_Balance,
        Messages.CodeCompletionPolicy_BalanceDescription),

    @SerializedName(CodeCompletionPolicy.CREATIVITY_ID)
    CREATIVITY(3, CodeCompletionPolicy.CREATIVITY_ID, Messages.CodeCompletionPolicy_Creativity,
        Messages.CodeCompletionPolicy_CreativityDescription);

    private static final String OFF_ID = "off"; //$NON-NLS-1$
    private static final String FOCUSING_ID = "focusing"; //$NON-NLS-1$
    private static final String BALANCE_ID = "balance"; //$NON-NLS-1$
    private static final String CREATIVITY_ID = "creativity"; //$NON-NLS-1$

    private final int index;
    private final String id;
    private final String name;
    private final String description;

    CodeCompletionPolicy(int index, String id, String name, String description)
    {
        this.index = index;
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getIndex()
    {
        return index;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isMeet(CodeCompletionPolicy policy)
    {
        return policy.getIndex() >= getIndex();
    }

    public static CodeCompletionPolicy parse(String id)
    {
        if (id == null)
        {
            return CodeCompletionPolicy.BALANCE;
        }

        switch (id.toLowerCase())
        {
        case OFF_ID:
            return OFF;

        case FOCUSING_ID:
            return FOCUSING;

        case BALANCE_ID:
            return BALANCE;

        case CREATIVITY_ID:
            return CREATIVITY;

        default:
            return BALANCE;
        }
    }
}
