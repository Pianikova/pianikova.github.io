/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Scenario-oriented JShell manual entry.
 * <p>
 * The {@code guide} field is loaded lazily through a {@link Supplier}, so providers can
 * keep guide content (e.g. markdown files) off the heap until a consumer actually
 * requests it via {@link #getGuide()}. The original eager-string constructor is preserved
 * for backwards compatibility.
 */
public class JShellManualEntry
{
    private final String id;
    private final String scope;
    private final String title;
    private final String summary;
    private final Supplier<String> guideSupplier;
    private final List<String> recommendedBindings;
    private final List<String> keywords;

    public JShellManualEntry(String id, String scope, String title, String summary, String guide,
        Collection<String> recommendedBindings, Collection<String> keywords)
    {
        this(id, scope, title, summary, () -> guide, recommendedBindings, keywords);
    }

    public JShellManualEntry(String id, String scope, String title, String summary, Supplier<String> guideSupplier,
        Collection<String> recommendedBindings, Collection<String> keywords)
    {
        this.id = id;
        this.scope = scope;
        this.title = title;
        this.summary = summary;
        this.guideSupplier = guideSupplier;
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
        return guideSupplier.get();
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
