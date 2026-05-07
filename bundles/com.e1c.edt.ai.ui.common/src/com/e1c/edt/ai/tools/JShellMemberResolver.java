/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JShellMemberResolver
    implements IJShellMemberResolver
{
    private final IJShellTypeIndex typeIndex;
    private final IWildcardMatcher wildcardMatcher;
    private final IReflectionSignatureFormatter signatureFormatter;

    @Inject
    public JShellMemberResolver(IJShellTypeIndex typeIndex, IWildcardMatcher wildcardMatcher,
        IReflectionSignatureFormatter signatureFormatter)
    {
        Preconditions.checkNotNull(typeIndex);
        Preconditions.checkNotNull(wildcardMatcher);
        Preconditions.checkNotNull(signatureFormatter);
        this.typeIndex = typeIndex;
        this.wildcardMatcher = wildcardMatcher;
        this.signatureFormatter = signatureFormatter;
    }

    @Override
    public List<JShellReflectionSearchResult> findMembers(IJShellSession session, String query, int resultLimit,
        int itemLimit)
    {
        var splitAt = query.lastIndexOf('.');
        if (splitAt <= 0 || splitAt == query.length() - 1)
        {
            return List.of();
        }

        var typeQuery = query.substring(0, splitAt);
        var memberQuery = query.substring(splitAt + 1);
        return typeIndex.findTypes(session, typeQuery, resultLimit).stream()
            .map(type -> toMemberResult(type, memberQuery, itemLimit))
            .filter(result -> !result.items.isEmpty())
            .collect(Collectors.toList());
    }

    private JShellReflectionSearchResult toMemberResult(JShellResolvedType resolvedType, String memberQuery, int itemLimit)
    {
        var type = resolvedType.getType();
        var items = new ArrayList<String>();

        Arrays.stream(type.getMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(method -> matches(memberQuery, method.getName()))
            .sorted(Comparator.comparing(signatureFormatter::format))
            .limit(itemLimit + 1L)
            .map(signatureFormatter::format)
            .forEach(items::add);

        if ("new".equals(memberQuery) || wildcardMatcher.matches(memberQuery, "new")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            Arrays.stream(type.getConstructors())
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .sorted(Comparator.comparing(signatureFormatter::format))
                .limit(itemLimit + 1L)
                .map(signatureFormatter::format)
                .forEach(items::add);
        }

        Arrays.stream(type.getFields())
            .filter(field -> Modifier.isPublic(field.getModifiers()))
            .filter(field -> matches(memberQuery, field.getName()))
            .sorted(Comparator.comparing(signatureFormatter::format))
            .limit(itemLimit + 1L)
            .map(signatureFormatter::format)
            .forEach(items::add);

        var result = new JShellReflectionSearchResult();
        result.kind = "member"; //$NON-NLS-1$
        result.fqn = resolvedType.getFqn();
        result.truncated = items.size() > itemLimit;
        result.items = items.stream().limit(itemLimit).collect(Collectors.toList());
        return result;
    }

    private boolean matches(String query, String name)
    {
        return wildcardMatcher.hasWildcard(query) ? wildcardMatcher.matches(query, name) : query.equals(name);
    }
}
