/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ICancellationToken;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JShellTypeIndex
    implements IJShellTypeIndex
{
    private final IWildcardMatcher wildcardMatcher;
    private final AtomicReference<List<String>> cachedTypeNames = new AtomicReference<>();
    private final AtomicReference<Map<String, List<String>>> cachedSimpleNameIndex = new AtomicReference<>();
    private final AtomicReference<Map<String, List<String>>> cachedSignificantSimpleNameIndex =
        new AtomicReference<>(Map.of());
    private final Object indexLock = new Object();

    @Inject
    public JShellTypeIndex(IWildcardMatcher wildcardMatcher)
    {
        Preconditions.checkNotNull(wildcardMatcher);
        this.wildcardMatcher = wildcardMatcher;
    }

    @Override
    public void warmUp(Collection<Class<?>> significantClasses, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(cancellationToken);
        if (significantClasses != null)
        {
            var significantTypeNames = significantClasses.stream()
                .filter(clazz -> clazz != null && !clazz.isPrimitive() && !clazz.isArray())
                .map(Class::getName)
                .filter(name -> !name.contains("$")) //$NON-NLS-1$
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            cachedSignificantSimpleNameIndex.set(buildSimpleNameIndex(significantTypeNames));
        }

        getSimpleNameIndex(cancellationToken);
    }

    @Override
    public List<JShellResolvedType> findTypes(IJShellSession session, String query, int limit)
    {
        var normalizedQuery = normalizeTypeQuery(query);
        if (normalizedQuery.isEmpty())
        {
            return List.of();
        }
        if (!wildcardMatcher.hasWildcard(normalizedQuery))
        {
            var directTypes = findDirectTypes(session, normalizedQuery, limit);
            if (!directTypes.isEmpty())
            {
                return directTypes;
            }
        }

        var names = candidateNames(session, normalizedQuery);
        return names.stream()
            .filter(name -> matchesTypeQuery(normalizedQuery, name))
            .sorted()
            .limit(limit)
            .map(name -> load(session, name))
            .filter(type -> type != null)
            .collect(Collectors.toList());
    }

    @Override
    public List<JShellResolvedType> findPackageTypes(IJShellSession session, String packageName, int limit)
    {
        var prefix = packageName + "."; //$NON-NLS-1$
        return getTypeNames().stream()
            .filter(name -> name.startsWith(prefix))
            .filter(name -> name.indexOf('.', prefix.length()) < 0)
            .sorted()
            .limit(limit)
            .map(name -> load(session, name))
            .filter(type -> type != null)
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasPackage(String packageName)
    {
        var prefix = packageName + "."; //$NON-NLS-1$
        return getTypeNames().stream().anyMatch(name -> name.startsWith(prefix));
    }

    private Collection<String> candidateNames(IJShellSession session, String query)
    {
        var names = new LinkedHashSet<String>();
        names.add(query);
        if (query.indexOf('.') >= 0 || wildcardMatcher.hasWildcard(query))
        {
            if (wildcardMatcher.hasWildcard(query))
            {
                names.addAll(getTypeNames());
            }
            return names;
        }

        for (var importStatement : session.getImports())
        {
            addImportCandidate(names, importStatement, query);
        }
        names.addAll(cachedSignificantSimpleNameIndex.get().getOrDefault(query, List.of()));
        names.addAll(getSimpleNameIndex(CancellationTokens.NONE).getOrDefault(query, List.of()));
        return names;
    }

    private List<JShellResolvedType> findDirectTypes(IJShellSession session, String query, int limit)
    {
        var names = directTypeNames(session, query);
        return names.stream()
            .map(name -> load(session, name))
            .filter(type -> type != null)
            .limit(limit)
            .collect(Collectors.toList());
    }

    private Collection<String> directTypeNames(IJShellSession session, String query)
    {
        var names = new LinkedHashSet<String>();
        names.add(query);
        if (query.indexOf('.') < 0)
        {
            names.add("java.lang." + query); //$NON-NLS-1$
            for (var importStatement : session.getImports())
            {
                addImportCandidate(names, importStatement, query);
            }
            names.addAll(cachedSignificantSimpleNameIndex.get().getOrDefault(query, List.of()));
        }
        return names;
    }

    private void addImportCandidate(Set<String> names, String importStatement, String simpleName)
    {
        var imp = importStatement == null ? "" : importStatement.trim(); //$NON-NLS-1$
        if (imp.startsWith("import ")) //$NON-NLS-1$
        {
            imp = imp.substring("import ".length()).trim(); //$NON-NLS-1$
        }
        if (imp.startsWith("static ")) //$NON-NLS-1$
        {
            imp = imp.substring("static ".length()).trim(); //$NON-NLS-1$
        }
        if (imp.endsWith(";")) //$NON-NLS-1$
        {
            imp = imp.substring(0, imp.length() - 1).trim();
        }
        if (imp.endsWith(".*")) //$NON-NLS-1$
        {
            names.add(imp.substring(0, imp.length() - 2) + "." + simpleName); //$NON-NLS-1$
        }
        else if (imp.endsWith("." + simpleName)) //$NON-NLS-1$
        {
            names.add(imp);
        }
    }

    private boolean matchesTypeQuery(String query, String fqn)
    {
        if (wildcardMatcher.hasWildcard(query))
        {
            return wildcardMatcher.matches(query, fqn) || wildcardMatcher.matches(query, simpleName(fqn));
        }
        return query.equals(fqn) || query.equals(simpleName(fqn));
    }

    private JShellResolvedType load(IJShellSession session, String fqn)
    {
        try
        {
            return new JShellResolvedType(fqn, Class.forName(fqn, false, session.getClassLoader()));
        }
        catch (ClassNotFoundException | LinkageError e)
        {
            return null;
        }
    }

    private List<String> getTypeNames()
    {
        return getTypeNames(CancellationTokens.NONE);
    }

    private List<String> getTypeNames(ICancellationToken cancellationToken)
    {
        var cached = cachedTypeNames.get();
        if (cached != null)
        {
            return cached;
        }

        synchronized (indexLock)
        {
            cached = cachedTypeNames.get();
            if (cached != null)
            {
                return cached;
            }

            var names = new ArrayList<String>();
            var anchor = FrameworkUtil.getBundle(JShellTypeIndex.class);
            if (anchor != null && anchor.getBundleContext() != null)
            {
                for (var bundle : anchor.getBundleContext().getBundles())
                {
                    if (cancellationToken.isCanceled())
                    {
                        return List.of();
                    }
                    addBundleTypes(names, bundle);
                }
            }
            var sorted = names.stream()
                .filter(name -> !name.contains("$")) //$NON-NLS-1$
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
            cachedTypeNames.set(List.copyOf(sorted));
            return cachedTypeNames.get();
        }
    }

    private Map<String, List<String>> getSimpleNameIndex(ICancellationToken cancellationToken)
    {
        var cached = cachedSimpleNameIndex.get();
        if (cached != null)
        {
            return cached;
        }

        synchronized (indexLock)
        {
            cached = cachedSimpleNameIndex.get();
            if (cached != null)
            {
                return cached;
            }

            var typeNames = getTypeNames(cancellationToken);
            if (cancellationToken.isCanceled())
            {
                return Map.of();
            }
            cachedSimpleNameIndex.set(buildSimpleNameIndex(typeNames));
            return cachedSimpleNameIndex.get();
        }
    }

    private Map<String, List<String>> buildSimpleNameIndex(List<String> typeNames)
    {
        var index = new HashMap<String, List<String>>();
        for (var name : typeNames)
        {
            index.computeIfAbsent(simpleName(name), key -> new ArrayList<>()).add(name);
        }
        return buildImmutableSimpleNameIndex(index);
    }

    private Map<String, List<String>> buildImmutableSimpleNameIndex(Map<String, List<String>> index)
    {
        var immutableIndex = new HashMap<String, List<String>>();
        for (var entry : index.entrySet())
        {
            immutableIndex.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(immutableIndex);
    }

    private void addBundleTypes(List<String> names, Bundle bundle)
    {
        // Lazy-activation bundles stay in STARTING until first class load, so a state whitelist
        // would drop their types; any bundle with a wiring is scannable.
        if (bundle == null || bundle.getState() == Bundle.UNINSTALLED)
        {
            return;
        }
        var wiring = bundle.adapt(BundleWiring.class);
        if (wiring == null)
        {
            return;
        }
        var resources = wiring.listResources("/", "*.class", //$NON-NLS-1$ //$NON-NLS-2$
            BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
        if (resources == null)
        {
            return;
        }
        for (var resource : resources)
        {
            if (resource.endsWith(".class")) //$NON-NLS-1$
            {
                names.add(resource.substring(0, resource.length() - ".class".length()).replace('/', '.')); //$NON-NLS-1$
            }
        }
    }

    private String normalizeTypeQuery(String query)
    {
        return query == null ? "" : query.trim(); //$NON-NLS-1$
    }

    private String simpleName(String fqn)
    {
        var index = fqn.lastIndexOf('.');
        return index >= 0 ? fqn.substring(index + 1) : fqn;
    }
}
