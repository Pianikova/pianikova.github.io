/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Loads markdown resources from a bundle classpath and resolves
 * {@code {{>fragment-id}}} includes and {@code {{var}}} placeholders.
 * <p>
 * Resource paths are resolved relative to the supplied anchor class via
 * {@link Class#getResourceAsStream(String)}. Fragment includes look up files
 * under {@code <root>/fragments/<id>.md}. Cycles in includes raise
 * {@link IllegalStateException}.
 */
public final class ManualResourceLoader
{
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("\\{\\{>\\s*([\\w\\-/]+)\\s*\\}\\}"); //$NON-NLS-1$
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([\\w\\.]+)\\s*\\}\\}"); //$NON-NLS-1$
    private static final Pattern DYN_PATTERN = Pattern.compile("\\{\\{\\$([\\w\\-]+):([\\w\\.]+)\\s*\\}\\}"); //$NON-NLS-1$

    private final Class<?> anchor;
    private final String root;
    private final Map<String, String> cache = new HashMap<>();
    private final Map<String, Function<String, String>> dynamicResolvers = new HashMap<>();

    /**
     * @param anchor class used to resolve resources; resources are read from this class's classloader
     * @param root resource root path (e.g. {@code "/manual"}). Must start with {@code /}.
     */
    public ManualResourceLoader(Class<?> anchor, String root)
    {
        if (anchor == null)
        {
            throw new IllegalArgumentException("anchor must not be null"); //$NON-NLS-1$
        }
        if (root == null || !root.startsWith("/")) //$NON-NLS-1$
        {
            throw new IllegalArgumentException("root must start with '/'"); //$NON-NLS-1$
        }
        this.anchor = anchor;
        this.root = root.endsWith("/") ? root.substring(0, root.length() - 1) : root; //$NON-NLS-1$
    }

    /**
     * Register a resolver for {@code {{$kind:argument}}} placeholders. Useful for
     * runtime-computed snippets such as method signature listings that cannot live
     * as static markdown.
     *
     * @param kind the placeholder kind (the part after {@code $} and before {@code :})
     * @param resolver function that maps the argument to its replacement text
     */
    public void registerDynamicResolver(String kind, Function<String, String> resolver)
    {
        dynamicResolvers.put(kind, resolver);
    }

    /**
     * Load resource content as raw text without any substitution. Cached.
     *
     * @param relativePath path relative to root (no leading slash)
     * @return raw file contents
     */
    public String loadRaw(String relativePath)
    {
        var cached = cache.get(relativePath);
        if (cached != null)
        {
            return cached;
        }
        var fullPath = root + "/" + relativePath; //$NON-NLS-1$
        try (InputStream stream = anchor.getResourceAsStream(fullPath))
        {
            if (stream == null)
            {
                throw new IllegalStateException("Resource not found: " + fullPath); //$NON-NLS-1$
            }
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
            {
                var content = reader.lines().collect(Collectors.joining("\n")); //$NON-NLS-1$
                cache.put(relativePath, content);
                return content;
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot read resource " + fullPath, e); //$NON-NLS-1$
        }
    }

    /**
     * Load a guide markdown with {@code {{>fragment}}} includes and {@code {{var}}} placeholders resolved.
     *
     * @param relativePath path relative to root (no leading slash)
     * @param vars optional variable map for {@code {{name}}} substitution; may be {@code null}
     * @return resolved markdown content
     */
    public String load(String relativePath, Map<String, String> vars)
    {
        var raw = loadRaw(relativePath);
        var withIncludes = resolveIncludes(raw, new HashMap<>());
        var withDynamic = resolveDynamic(withIncludes);
        return resolveVars(withDynamic, vars);
    }

    private String resolveDynamic(String content)
    {
        if (dynamicResolvers.isEmpty())
        {
            return content;
        }
        var matcher = DYN_PATTERN.matcher(content);
        var out = new StringBuilder();
        var last = 0;
        while (matcher.find())
        {
            out.append(content, last, matcher.start());
            var kind = matcher.group(1);
            var arg = matcher.group(2);
            var resolver = dynamicResolvers.get(kind);
            out.append(resolver != null ? resolver.apply(arg) : matcher.group());
            last = matcher.end();
        }
        out.append(content, last, content.length());
        return out.toString();
    }

    private String resolveIncludes(String content, Map<String, Boolean> visiting)
    {
        var matcher = INCLUDE_PATTERN.matcher(content);
        var out = new StringBuilder();
        var last = 0;
        while (matcher.find())
        {
            out.append(content, last, matcher.start());
            var id = matcher.group(1);
            if (Boolean.TRUE.equals(visiting.get(id)))
            {
                throw new IllegalStateException("Cyclic fragment include: " + id); //$NON-NLS-1$
            }
            visiting.put(id, Boolean.TRUE);
            var fragmentPath = "fragments/" + id + ".md"; //$NON-NLS-1$ //$NON-NLS-2$
            var fragment = resolveIncludes(loadRaw(fragmentPath), visiting);
            visiting.remove(id);
            out.append(fragment);
            last = matcher.end();
        }
        out.append(content, last, content.length());
        return out.toString();
    }

    private String resolveVars(String content, Map<String, String> vars)
    {
        if (vars == null || vars.isEmpty())
        {
            return content;
        }
        var matcher = VAR_PATTERN.matcher(content);
        var out = new StringBuilder();
        var last = 0;
        while (matcher.find())
        {
            out.append(content, last, matcher.start());
            var key = matcher.group(1);
            var value = vars.get(key);
            out.append(value != null ? value : matcher.group());
            last = matcher.end();
        }
        out.append(content, last, content.length());
        return out.toString();
    }
}
