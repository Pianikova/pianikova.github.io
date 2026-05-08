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
    private final String category;
    private final String title;
    private final String summary;
    private final Supplier<String> guideSupplier;
    private final List<String> recommendedBindings;
    private final List<String> keywords;

    public JShellManualEntry(String id, String scope, String title, String summary, String guide,
        Collection<String> recommendedBindings, Collection<String> keywords)
    {
        this(id, scope, deriveCategory(id), title, summary, () -> guide, recommendedBindings, keywords);
    }

    public JShellManualEntry(String id, String scope, String title, String summary, Supplier<String> guideSupplier,
        Collection<String> recommendedBindings, Collection<String> keywords)
    {
        this(id, scope, deriveCategory(id), title, summary, guideSupplier, recommendedBindings, keywords);
    }

    public JShellManualEntry(String id, String scope, String category, String title, String summary,
        Supplier<String> guideSupplier, Collection<String> recommendedBindings, Collection<String> keywords)
    {
        this.id = id;
        this.scope = scope;
        this.category = category != null ? category : deriveCategory(id);
        this.title = title;
        this.summary = summary;
        this.guideSupplier = guideSupplier;
        this.recommendedBindings = List.copyOf(recommendedBindings);
        this.keywords = List.copyOf(keywords);
    }

    /**
     * Derive a category from a scenario id. Used when the entry source did not
     * supply an explicit category. Buckets: {@code create}, {@code edit},
     * {@code delete}, {@code composite}, {@code enhanced}, {@code reference},
     * {@code configuration}, {@code misc}.
     */
    @SuppressWarnings("nls")
    public static String deriveCategory(String id)
    {
        if (id == null)
        {
            return "misc";
        }
        if (id.endsWith("_configuration_project") || id.equals("create_configuration_project")
            || id.equals("delete_configuration_project"))
        {
            return "configuration";
        }
        if (id.startsWith("enhanced_"))
        {
            return "enhanced";
        }
        if (id.startsWith("create_"))
        {
            return "create";
        }
        if (id.startsWith("edit_"))
        {
            return "edit";
        }
        if (id.startsWith("delete_"))
        {
            return "delete";
        }
        if (id.startsWith("add_") || id.startsWith("rename_") || id.startsWith("resolve_")
            || id.startsWith("set_"))
        {
            return "composite";
        }
        if (id.equals("validation_errors") || id.equals("safe_uuid_assignment")
            || id.equals("typedescription_best_practices") || id.equals("child_elements_uuid_importance")
            || id.equals("edt_overview"))
        {
            return "reference";
        }
        return "misc";
    }

    public String getId()
    {
        return id;
    }

    public String getScope()
    {
        return scope;
    }

    public String getCategory()
    {
        return category;
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
