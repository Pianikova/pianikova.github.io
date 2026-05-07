/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

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
public class JShellReflectionService
    implements IJShellReflectionService
{
    private static final int RESULT_LIMIT = 20;
    private static final int ITEM_LIMIT = 80;
    private static final int TYPE_MEMBER_LIMIT = 40;
    private static final int SUGGESTION_LIMIT = 8;

    private final IJShellTypeIndex typeIndex;
    private final IJShellMemberResolver memberResolver;
    private final IReflectionSignatureFormatter signatureFormatter;
    private final IJShellReflectionQuerySuggester querySuggester;
    private final Cache<Class<?>, List<String>> publicMembersCache =
        CacheBuilder.newBuilder().maximumSize(2048).weakKeys().build();

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
        return queries.stream().map(query -> search(session, query)).collect(Collectors.toList());
    }

    private JShellReflectionQueryResult search(IJShellSession session, String query)
    {
        var normalized = query == null ? "" : query.trim(); //$NON-NLS-1$
        var result = new JShellReflectionQueryResult();
        result.query = query;
        result.results = new ArrayList<>();
        result.suggestions = List.of();

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
            .sorted(Comparator.comparing(signatureFormatter::format))
            .limit(TYPE_MEMBER_LIMIT)
            .map(signatureFormatter::format)
            .forEach(items::add);
        Arrays.stream(type.getFields())
            .sorted(Comparator.comparing(signatureFormatter::format))
            .limit(TYPE_MEMBER_LIMIT)
            .map(signatureFormatter::format)
            .forEach(items::add);
        return items.stream().limit(TYPE_MEMBER_LIMIT).collect(Collectors.toList());
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
}
