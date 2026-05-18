/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.inject.Singleton;

@Singleton
@SuppressWarnings("nls")
public class JShellReflectionQuerySuggester
    implements IJShellReflectionQuerySuggester
{
    private static final Pattern MEMBER_CALL_PATTERN =
        Pattern.compile("\\b([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)+)\\s*\\("); //$NON-NLS-1$

    private static final Pattern NEW_MEMBER_CALL_PATTERN =
        Pattern.compile("\\bnew\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*\\([^)]*\\)\\s*\\.\\s*([A-Za-z_$][\\w$]*)\\s*\\("); //$NON-NLS-1$

    private static final Pattern COMPILER_SYMBOL_PATTERN =
        Pattern.compile("(?i)(?:symbol|символ)\\s*:\\s*(?:method|variable|class|метод|переменная|класс)?\\s*([\\w$]+)"); //$NON-NLS-1$

    private static final Pattern COMPILER_LOCATION_CLASS_PATTERN =
        Pattern.compile("(?i)location\\s*:\\s*class\\s+([\\w.$]+)"); //$NON-NLS-1$

    @Override
    public List<String> suggestForQuery(String query, int limit)
    {
        var suggestions = new LinkedHashSet<String>();
        addQuerySuggestions(suggestions, normalize(query));
        return limit(suggestions, limit);
    }

    @Override
    public List<String> suggestForCompilationErrors(String code, List<CompilationError> errors, int limit)
    {
        var suggestions = new LinkedHashSet<String>();
        if (code != null)
        {
            addMemberCallSuggestions(suggestions, code);
        }
        if (errors != null)
        {
            for (var error : errors)
            {
                addErrorMessageSuggestions(suggestions, error == null ? null : error.message);
            }
        }
        return limit(suggestions, limit);
    }

    private void addQuerySuggestions(Set<String> suggestions, String query)
    {
        if (query.isEmpty())
        {
            return;
        }

        var splitAt = query.lastIndexOf('.');
        if (splitAt > 0 && splitAt < query.length() - 1)
        {
            var owner = query.substring(0, splitAt);
            var name = stripWildcards(query.substring(splitAt + 1));
            if (!name.isEmpty())
            {
                suggestions.add(owner + ".*" + name + "*"); //$NON-NLS-1$ //$NON-NLS-2$
                suggestions.add(owner + ".set*" + capitalize(name) + "*"); //$NON-NLS-1$ //$NON-NLS-2$
                suggestions.add(owner + ".get*" + capitalize(name) + "*"); //$NON-NLS-1$ //$NON-NLS-2$
                suggestions.add(owner + ".is*" + capitalize(name) + "*"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            var simpleName = stripWildcards(query.substring(splitAt + 1));
            if (!simpleName.isEmpty())
            {
                suggestions.add(owner + ".*" + simpleName + "*"); //$NON-NLS-1$ //$NON-NLS-2$
                suggestions.add("*" + simpleName + "*"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return;
        }

        var name = stripWildcards(query);
        if (!name.isEmpty())
        {
            suggestions.add("*" + name + "*"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void addMemberCallSuggestions(Set<String> suggestions, String code)
    {
        var newMatcher = NEW_MEMBER_CALL_PATTERN.matcher(code);
        while (newMatcher.find())
        {
            addQuerySuggestions(suggestions, newMatcher.group(1) + "." + newMatcher.group(2)); //$NON-NLS-1$
        }

        var matcher = MEMBER_CALL_PATTERN.matcher(code);
        while (matcher.find())
        {
            addQuerySuggestions(suggestions, matcher.group(1));
        }
    }

    private void addErrorMessageSuggestions(Set<String> suggestions, String message)
    {
        if (message == null || message.isBlank())
        {
            return;
        }

        var locationMatcher = COMPILER_LOCATION_CLASS_PATTERN.matcher(message);
        while (locationMatcher.find())
        {
            var location = locationMatcher.group(1);
            if (location != null && !location.isBlank())
            {
                var simpleLocation = location.substring(location.lastIndexOf('.') + 1);
                suggestions.add(location + ".*"); //$NON-NLS-1$
                suggestions.add(simpleLocation + ".*"); //$NON-NLS-1$
            }
        }

        var matcher = COMPILER_SYMBOL_PATTERN.matcher(message);
        while (matcher.find())
        {
            var symbol = matcher.group(1);
            if (symbol != null && !symbol.isBlank())
            {
                suggestions.add("*" + symbol + "*"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    private List<String> limit(LinkedHashSet<String> suggestions, int limit)
    {
        return suggestions.stream()
            .filter(suggestion -> suggestion != null && !suggestion.isBlank())
            .limit(Math.max(0, limit))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private String normalize(String value)
    {
        return value == null ? "" : value.trim(); //$NON-NLS-1$
    }

    private String stripWildcards(String value)
    {
        return normalize(value).replace("*", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String capitalize(String value)
    {
        if (value.isEmpty())
        {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
