/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Минимальный уровень серьёзности проблем, которые фоновый анализ помечает маркерами.
 * Работает как порог: {@link #WARNING} означает «только предупреждения и ошибки»,
 * {@link #ERROR} — «только ошибки», {@link #INFORMATION} — все.
 */
public enum ProblemLevel
{
    INFORMATION(0, ProblemLevel.INFORMATION_ID, "info"), //$NON-NLS-1$
    WARNING(1, ProblemLevel.WARNING_ID, "warning"), //$NON-NLS-1$
    ERROR(2, ProblemLevel.ERROR_ID, "error"); //$NON-NLS-1$

    public static final String INFORMATION_ID = "information"; //$NON-NLS-1$
    public static final String WARNING_ID = "warning"; //$NON-NLS-1$
    public static final String ERROR_ID = "error"; //$NON-NLS-1$

    private final int index;
    private final String id;
    // Токен severity в терминах скилла (info/warning/error) для этого уровня.
    private final String severityToken;

    ProblemLevel(int index, String id, String severityToken)
    {
        this.index = index;
        this.id = id;
        this.severityToken = severityToken;
    }

    public int getIndex()
    {
        return index;
    }

    public String getId()
    {
        return id;
    }

    /**
     * Скилловые токены severity (info/warning/error), которые допустимо помечать при этом пороге,
     * через запятую, от менее к более серьёзному. Например, для {@link #WARNING} — {@code "warning, error"}.
     */
    public String getAllowedSeverities()
    {
        return Arrays.stream(values())
            .filter(level -> level.index >= this.index)
            .map(level -> level.severityToken)
            .collect(Collectors.joining(", ")); //$NON-NLS-1$
    }

    public static ProblemLevel parse(String id)
    {
        if (id != null)
        {
            switch (id.toLowerCase())
            {
            case INFORMATION_ID:
                return INFORMATION;
            case WARNING_ID:
                return WARNING;
            case ERROR_ID:
                return ERROR;
            default:
                break;
            }
        }
        return WARNING;
    }
}
