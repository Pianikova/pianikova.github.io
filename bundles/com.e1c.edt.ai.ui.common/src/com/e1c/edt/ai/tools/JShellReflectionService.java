/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JShellReflectionService
    implements IJShellReflectionService
{
    private static final int RESULT_LIMIT = 20;
    private static final int ITEM_LIMIT = 5000;
    private static final int SUGGESTION_LIMIT = 16;

    private final IJShellTypeIndex typeIndex;
    private final IJShellMemberResolver memberResolver;
    private final IReflectionSignatureFormatter signatureFormatter;
    private final IJShellReflectionQuerySuggester querySuggester;
    private final Cache<Class<?>, List<String>> publicMembersCache =
        CacheBuilder.newBuilder().maximumSize(8192).weakKeys().build();
    private final Cache<String, JShellReflectionQueryResult> queryResultCache =
        CacheBuilder.newBuilder().maximumSize(8192).build();

    @Inject
    public JShellReflectionService(IJShellTypeIndex typeIndex, IJShellMemberResolver memberResolver,
        IReflectionSignatureFormatter signatureFormatter, IJShellReflectionQuerySuggester querySuggester)
    {
        Preconditions.checkNotNull(typeIndex);
        Preconditions.checkNotNull(memberResolver);
        Preconditions.checkNotNull(signatureFormatter);
        Preconditions.checkNotNull(querySuggester);
        this.typeIndex = typeIndex;
        this.memberResolver = memberResolver;
        this.signatureFormatter = signatureFormatter;
        this.querySuggester = querySuggester;
    }

    @Override
    public List<JShellReflectionQueryResult> search(IJShellSession session, List<String> queries)
    {
        if (queries == null)
        {
            return List.of();
        }
        return queries.parallelStream().map(query -> search(session, query)).collect(Collectors.toList());
    }

    private JShellReflectionQueryResult search(IJShellSession session, String query)
    {
        var normalized = normalizeQuery(query);
        var cacheKey = System.identityHashCode(session.getClassLoader()) + ":" + normalized; //$NON-NLS-1$
        var cached = queryResultCache.getIfPresent(cacheKey);
        if (cached != null)
        {
            return cached;
        }
        var result = searchUncached(session, query, normalized);
        queryResultCache.put(cacheKey, result);
        return result;
    }

    private String normalizeQuery(String query)
    {
        var normalized = query == null ? "" : query.trim(); //$NON-NLS-1$
        if (normalized.startsWith("import ")) //$NON-NLS-1$
        {
            normalized = normalized.substring("import ".length()).trim(); //$NON-NLS-1$
            if (normalized.startsWith("static ")) //$NON-NLS-1$
            {
                normalized = normalized.substring("static ".length()).trim(); //$NON-NLS-1$
            }
            if (normalized.endsWith(";")) //$NON-NLS-1$
            {
                normalized = normalized.substring(0, normalized.length() - 1).trim();
            }
            if (normalized.endsWith(".*")) //$NON-NLS-1$
            {
                normalized = normalized.substring(0, normalized.length() - 2);
            }
        }
        return normalized;
    }

    private JShellReflectionQueryResult searchUncached(IJShellSession session, String query, String normalized)
    {
        var result = new JShellReflectionQueryResult();
        result.query = query;
        result.results = new ArrayList<>();
        result.suggestions = List.of();

        if (!normalized.isEmpty() && normalized.indexOf('.') >= 0 && !normalized.endsWith(".*")) //$NON-NLS-1$
        {
            var exactTypes = typeIndex.findTypes(session, normalized, RESULT_LIMIT + 1);
            if (!exactTypes.isEmpty() && exactTypes.stream().anyMatch(type -> normalized.equals(type.getFqn())))
            {
                result.kind = "type-search"; //$NON-NLS-1$
                result.truncated = exactTypes.size() > RESULT_LIMIT;
                result.results = exactTypes.stream()
                    .filter(type -> normalized.equals(type.getFqn()))
                    .limit(RESULT_LIMIT)
                    .map(this::toTypeResult)
                    .collect(Collectors.toList());
                return result;
            }
        }

        var memberResults = memberResolver.findMembers(session, normalized, RESULT_LIMIT, ITEM_LIMIT);
        if (!memberResults.isEmpty())
        {
            result.kind = "member-search"; //$NON-NLS-1$
            result.results = memberResults;
            result.truncated = memberResults.size() >= RESULT_LIMIT || memberResults.stream().anyMatch(r -> r.truncated);
            return result;
        }

        var types = typeIndex.findTypes(session, normalized, RESULT_LIMIT + 1);
        if (!types.isEmpty())
        {
            result.kind = "type-search"; //$NON-NLS-1$
            result.truncated = types.size() > RESULT_LIMIT;
            result.results = types.stream().limit(RESULT_LIMIT).map(this::toTypeResult).collect(Collectors.toList());
            return result;
        }

        if (typeIndex.hasPackage(normalized))
        {
            var packageTypes = typeIndex.findPackageTypes(session, normalized, ITEM_LIMIT + 1);
            var packageResult = new JShellReflectionSearchResult();
            packageResult.kind = "package"; //$NON-NLS-1$
            packageResult.fqn = normalized;
            packageResult.truncated = packageTypes.size() > ITEM_LIMIT;
            packageResult.items = packageTypes.stream()
                .limit(ITEM_LIMIT)
                .map(type -> type.getSimpleName() + " (" + kindOf(type.getType()) + ")") //$NON-NLS-1$ //$NON-NLS-2$
                .collect(Collectors.toList());

            result.kind = "package-search"; //$NON-NLS-1$
            result.results = List.of(packageResult);
            result.truncated = packageResult.truncated;
            return result;
        }

        result.kind = "not-found"; //$NON-NLS-1$
        result.suggestions = querySuggester.suggestForQuery(normalized, SUGGESTION_LIMIT);
        return result;
    }

    private JShellReflectionSearchResult toTypeResult(JShellResolvedType resolvedType)
    {
        var type = resolvedType.getType();
        var result = new JShellReflectionSearchResult();
        result.kind = kindOf(type);
        result.fqn = resolvedType.getFqn();
        if (type.isEnum())
        {
            result.items = Arrays.stream(type.getEnumConstants())
                .map(String::valueOf)
                .collect(Collectors.toList());
        }
        else
        {
            result.items = publicMembers(type);
        }
        result.truncated = false;
        return result;
    }

    private List<String> publicMembers(Class<?> type)
    {
        try
        {
            return publicMembersCache.get(type, () -> buildPublicMembers(type));
        }
        catch (ExecutionException e)
        {
            return buildPublicMembers(type);
        }
    }

    private List<String> buildPublicMembers(Class<?> type)
    {
        var items = new ArrayList<String>();
        Arrays.stream(type.getMethods())
            .sorted(memberComparator(type))
            .map(signatureFormatter::format)
            .forEach(items::add);
        Arrays.stream(type.getFields())
            .sorted(memberComparator(type))
            .map(signatureFormatter::format)
            .forEach(items::add);
        return List.copyOf(items);
    }

    private <T extends java.lang.reflect.Member> Comparator<T> memberComparator(Class<?> owner)
    {
        return Comparator
            .comparingInt((T member) -> inheritanceDistance(owner, member.getDeclaringClass()))
            .thenComparingInt(member -> memberPriority(member.getName()))
            .thenComparing(this::formatMember);
    }

    private String formatMember(java.lang.reflect.Member member)
    {
        if (member instanceof java.lang.reflect.Method)
        {
            return signatureFormatter.format((java.lang.reflect.Method)member);
        }
        if (member instanceof java.lang.reflect.Field)
        {
            return signatureFormatter.format((java.lang.reflect.Field)member);
        }
        return member.getName();
    }

    private int memberPriority(String name)
    {
        if (name.startsWith("get") || name.startsWith("set") || name.startsWith("is")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        {
            return 0;
        }
        if (name.startsWith("create")) //$NON-NLS-1$
        {
            return 1;
        }
        if (name.startsWith("e")) //$NON-NLS-1$
        {
            return 3;
        }
        return 2;
    }

    private int inheritanceDistance(Class<?> owner, Class<?> declaringClass)
    {
        if (owner == null || declaringClass == null)
        {
            return Integer.MAX_VALUE;
        }
        if (owner.equals(declaringClass))
        {
            return 0;
        }

        var visited = new HashSet<Class<?>>();
        var queue = new ArrayDeque<TypeDistance>();
        queue.add(new TypeDistance(owner, 0));
        while (!queue.isEmpty())
        {
            var current = queue.removeFirst();
            if (current.type == null || !visited.add(current.type))
            {
                continue;
            }
            if (current.type.equals(declaringClass))
            {
                return current.distance;
            }
            var superclass = current.type.getSuperclass();
            if (superclass != null)
            {
                queue.addLast(new TypeDistance(superclass, current.distance + 1));
            }
            for (var iface : current.type.getInterfaces())
            {
                queue.addLast(new TypeDistance(iface, current.distance + 1));
            }
        }
        return Integer.MAX_VALUE;
    }

    private String kindOf(Class<?> type)
    {
        if (type.isEnum())
        {
            return "enum"; //$NON-NLS-1$
        }
        if (type.isAnnotation())
        {
            return "annotation"; //$NON-NLS-1$
        }
        if (type.isInterface())
        {
            return "interface"; //$NON-NLS-1$
        }
        return "class"; //$NON-NLS-1$
    }

    private static final class TypeDistance
    {
        final Class<?> type;
        final int distance;

        TypeDistance(Class<?> type, int distance)
        {
            this.type = type;
            this.distance = distance;
        }
    }
}
