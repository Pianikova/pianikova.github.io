/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Collection;
import java.util.List;

/**
 * Scenario-oriented JShell manual entry.
 */
public class JShellManualEntry
{
    private final String id;
    private final String scope;
    private final String title;
    private final String summary;
    private final String guide;
    private final List<String> recommendedBindings;
    private final List<String> keywords;

    public JShellManualEntry(String id, String scope, String title, String summary, String guide,
        Collection<String> recommendedBindings, Collection<String> keywords)
    {
        this.id = id;
        this.scope = scope;
        this.title = title;
        this.summary = summary;
        this.guide = guide;
        this.recommendedBindings = List.copyOf(recommendedBindings);
        this.keywords = List.copyOf(keywords);
    }

    public String getId()
    {
        return id;
    }

    public String getScope()
    {
        return scope;
    }

    public String getTitle()
    {
        return title;
    }

    public String getSummary()
    {
        return summary;
    }

    public String getGuide()
    {
        return guide;
    }

    public List<String> getRecommendedBindings()
    {
        return recommendedBindings;
    }

    public List<String> getKeywords()
    {
        return keywords;
    }
}
