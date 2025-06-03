/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent.model;

import com.e1c.edt.ai.Messages;
import com.google.gson.annotations.SerializedName;

/**
 * Перечисление литик завершения кода.
 */
public enum CodeCompletionPolicy
{
    /**
     * Завершение кода выключено (на чат не влияет).
     */
    @SerializedName(CodeCompletionPolicy.OFF_ID)
    OFF(0, CodeCompletionPolicy.OFF_ID, Messages.CodeCompletionPolicy_Off,
        Messages.CodeCompletionPolicy_OffShortDescription,
        Messages.CodeCompletionPolicy_OffDescription),

    /**
     * Завершение кода работает только по требованию - горячие клавиши, интеграция с context assist выключена.
     */
    @SerializedName(CodeCompletionPolicy.MANUAL_ID)
    MANUAL(1, CodeCompletionPolicy.MANUAL_ID, Messages.CodeCompletionPolicy_Manual,
        Messages.CodeCompletionPolicy_ManualShortDescription,
        Messages.CodeCompletionPolicy_ManualDescription),

    /**
     * Завершение кода работает в режиме баланса. Если предлагается только форматирование, то такие предложения игнорируются. Интеграция с context assist включена.
     */
    @SerializedName(CodeCompletionPolicy.MODERATE_ID)
    MODERATE(2, CodeCompletionPolicy.MODERATE_ID, Messages.CodeCompletionPolicy_Moderate,
        Messages.CodeCompletionPolicy_ModerateShortDescription,
        Messages.CodeCompletionPolicy_ModerateDescription),

    /**
     * Завершение кода работает в режиме творчества. Завершение кода работает активнее, уменьшено ожидание пауз при печати пользователем, показываются все предложения. Интеграция с context assist включена.
     */
    @SerializedName(CodeCompletionPolicy.INTENSVE_ID)
    INTENSVE(3, CodeCompletionPolicy.INTENSVE_ID, Messages.CodeCompletionPolicy_Intensive,
        Messages.CodeCompletionPolicy_IntensiveShortDescription,
        Messages.CodeCompletionPolicy_IntensiveDescription);

    private static final String OFF_ID = "off"; //$NON-NLS-1$
    private static final String MANUAL_ID = "manual"; //$NON-NLS-1$
    private static final String MODERATE_ID = "moderate"; //$NON-NLS-1$
    private static final String INTENSVE_ID = "intensive"; //$NON-NLS-1$
    private static final String LONG_NAME_SEPARATOR = " - "; //$NON-NLS-1$

    private final int index;
    private final String id, name, longName, shortDescription, description;

    CodeCompletionPolicy(int index, String id, String name, String shortDescription, String description)
    {
        this.index = index;
        this.id = id;
        this.name = name;
        this.shortDescription = shortDescription;
        this.description = description;
        if (shortDescription.isBlank())
        {
            longName = name;
        }
        else
        {
            var longName = new StringBuilder(name.length() + LONG_NAME_SEPARATOR.length() + shortDescription.length());
            longName.append(name);
            longName.append(LONG_NAME_SEPARATOR);
            longName.append(shortDescription);
            this.longName = longName.toString();
        }
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

    public String getLongName()
    {
        return longName;
    }

    public String getShortDescription()
    {
        return shortDescription;
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
            return CodeCompletionPolicy.MODERATE;
        }

        switch (id.toLowerCase())
        {
        case OFF_ID:
            return OFF;

        case MANUAL_ID:
            return MANUAL;

        case MODERATE_ID:
            return MODERATE;

        case INTENSVE_ID:
            return INTENSVE;

        default:
            return MODERATE;
        }
    }
}
