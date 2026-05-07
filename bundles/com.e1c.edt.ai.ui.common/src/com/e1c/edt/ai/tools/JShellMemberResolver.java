/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JShellMemberResolver
    implements IJShellMemberResolver
{
    private final IJShellTypeIndex typeIndex;
    private final IWildcardMatcher wildcardMatcher;
    private final IReflectionSignatureFormatter signatureFormatter;
    private final Cache<Class<?>, TypeMembers> membersCache =
        CacheBuilder.newBuilder().maximumSize(8192).weakKeys().build();

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
        if (type.isEnum() && "*".equals(memberQuery)) //$NON-NLS-1$
        {
            return toEnumConstantsResult(resolvedType, itemLimit);
        }

        var members = getMembers(type);
        var items = new ArrayList<String>();

        members.methods.stream()
            .filter(method -> matches(memberQuery, method.getName()))
            .limit(itemLimit + 1L)
            .map(signatureFormatter::format)
            .forEach(items::add);

        if ("new".equals(memberQuery) || wildcardMatcher.matches(memberQuery, "new")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            members.constructors.stream()
                .limit(itemLimit + 1L)
                .map(signatureFormatter::format)
                .forEach(items::add);
        }

        members.fields.stream()
            .filter(field -> matches(memberQuery, field.getName()))
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

    private JShellReflectionSearchResult toEnumConstantsResult(JShellResolvedType resolvedType, int itemLimit)
    {
        var items = Arrays.stream(resolvedType.getType().getEnumConstants())
            .map(String::valueOf)
            .collect(Collectors.toList());

        var result = new JShellReflectionSearchResult();
        result.kind = "enum"; //$NON-NLS-1$
        result.fqn = resolvedType.getFqn();
        result.truncated = items.size() > itemLimit;
        result.items = items.stream().limit(itemLimit).collect(Collectors.toList());
        return result;
    }

    private boolean matches(String query, String name)
    {
        return wildcardMatcher.hasWildcard(query) ? wildcardMatcher.matches(query, name) : query.equals(name);
    }

    private TypeMembers getMembers(Class<?> type)
    {
        try
        {
            return membersCache.get(type, () -> buildMembers(type));
        }
        catch (ExecutionException e)
        {
            return buildMembers(type);
        }
    }

    private TypeMembers buildMembers(Class<?> type)
    {
        var methods = Arrays.stream(type.getMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .sorted(Comparator.comparing(signatureFormatter::format))
            .collect(Collectors.toUnmodifiableList());
        var constructors = Arrays.stream(type.getConstructors())
            .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
            .sorted(Comparator.comparing(signatureFormatter::format))
            .collect(Collectors.toUnmodifiableList());
        var fields = Arrays.stream(type.getFields())
            .filter(field -> Modifier.isPublic(field.getModifiers()))
            .sorted(Comparator.comparing(signatureFormatter::format))
            .collect(Collectors.toUnmodifiableList());
        return new TypeMembers(methods, constructors, fields);
    }

    private static class TypeMembers
    {
        final List<Method> methods;
        final List<Constructor<?>> constructors;
        final List<Field> fields;

        TypeMembers(List<Method> methods, List<Constructor<?>> constructors, List<Field> fields)
        {
            this.methods = methods;
            this.constructors = constructors;
            this.fields = fields;
        }
    }
}
