/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

/**
 * Declarative contract used to distinguish a final skill answer from an intermediate textual
 * response.
 *
 * @author Skill Test
 */
public final class SkillCompletionPolicy
{
    private final String marker;
    private final boolean rejectToolLikeJson;

    public SkillCompletionPolicy(String marker, boolean rejectToolLikeJson)
    {
        if (marker == null || marker.isBlank())
        {
            throw new IllegalArgumentException("Completion marker must not be blank"); //$NON-NLS-1$
        }
        this.marker = marker;
        this.rejectToolLikeJson = rejectToolLikeJson;
    }

    public String getMarker()
    {
        return marker;
    }

    public boolean isRejectToolLikeJson()
    {
        return rejectToolLikeJson;
    }
}
