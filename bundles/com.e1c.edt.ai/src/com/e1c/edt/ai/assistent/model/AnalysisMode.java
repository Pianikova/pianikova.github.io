/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

/**
 * Режим фонового анализа кода. Определяет, под каким conversation skill выполняется ревью:
 * {@link #STANDARD} — лёгкий режим {@code raw}, {@link #ADVANCED} — агентный режим {@code custom}
 * (с is_chat), который строже следует шагам скилла.
 */
public enum AnalysisMode
{
    STANDARD(AnalysisMode.STANDARD_ID, "raw"), //$NON-NLS-1$
    ADVANCED(AnalysisMode.ADVANCED_ID, "custom"); //$NON-NLS-1$

    public static final String STANDARD_ID = "standard"; //$NON-NLS-1$
    public static final String ADVANCED_ID = "advanced"; //$NON-NLS-1$

    private final String id;
    private final String skillName;

    AnalysisMode(String id, String skillName)
    {
        this.id = id;
        this.skillName = skillName;
    }

    public String getId()
    {
        return id;
    }

    /**
     * Имя conversation skill ({@code raw}/{@code custom}), под которым запускается диалог ревью.
     */
    public String getSkillName()
    {
        return skillName;
    }

    public static AnalysisMode parse(String id)
    {
        if (id != null && ADVANCED_ID.equalsIgnoreCase(id))
        {
            return ADVANCED;
        }
        return STANDARD;
    }
}
