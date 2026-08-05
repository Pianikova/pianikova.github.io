/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillMetadata
{
    private static final String ALLOWED_TOOLS = "allowed-tools"; //$NON-NLS-1$
    private static final String COMPLETION_MARKER = "completion-marker"; //$NON-NLS-1$
    private static final String REJECT_TOOL_LIKE_JSON = "reject-tool-like-json"; //$NON-NLS-1$

    private final Map<String, String> values;

    public SkillMetadata(Map<String, String> values)
    {
        this.values = values == null ? Collections.emptyMap() : Collections.unmodifiableMap(values);
    }

    public String get(String key)
    {
        return values.get(key);
    }

    public Map<String, String> getValues()
    {
        return values;
    }

    /**
     * Returns the tool allowlist declared by the Agent Skills compatible
     * {@code allowed-tools} frontmatter field.
     * <p>
     * A missing field or {@code *} means that all available tools are exposed.
     * Space-separated values are the canonical format; commas and YAML-style
     * inline arrays are accepted for compatibility.
     * </p>
     *
     * @return an empty optional when tools are unrestricted, otherwise the declared tool names
     */
    public Optional<Set<String>> getAllowedTools()
    {
        if (!values.containsKey(ALLOWED_TOOLS))
        {
            return Optional.empty();
        }

        var value = values.get(ALLOWED_TOOLS);
        if (value == null)
        {
            return Optional.of(Collections.emptySet());
        }

        var normalized = stripQuotes(value.trim());
        if ("*".equals(normalized)) //$NON-NLS-1$
        {
            return Optional.empty();
        }
        if (normalized.startsWith("[") && normalized.endsWith("]")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        var tools = new LinkedHashSet<String>();
        for (var item : normalized.split("[,\\s]+")) //$NON-NLS-1$
        {
            var tool = stripQuotes(item.trim());
            if (!tool.isEmpty())
            {
                tools.add(tool);
            }
        }
        return Optional.of(Collections.unmodifiableSet(tools));
    }

    /**
     * Returns the optional final-answer protocol declared by the skill.
     *
     * @return completion policy, or empty when the skill uses the ordinary stop response
     */
    public Optional<SkillCompletionPolicy> getCompletionPolicy()
    {
        var rawMarker = values.get(COMPLETION_MARKER);
        if (rawMarker == null || rawMarker.isBlank())
        {
            return Optional.empty();
        }
        var marker = stripQuotes(rawMarker.trim());
        var rejectToolLikeJson = Boolean.parseBoolean(stripQuotes(
            values.getOrDefault(REJECT_TOOL_LIKE_JSON, Boolean.FALSE.toString()).trim()));
        return Optional.of(new SkillCompletionPolicy(marker, rejectToolLikeJson));
    }

    private static String stripQuotes(String value)
    {
        if (value.length() >= 2)
        {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\''))
            {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
