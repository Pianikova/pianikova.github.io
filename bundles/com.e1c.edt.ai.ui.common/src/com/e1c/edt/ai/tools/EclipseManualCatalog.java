/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.e1c.edt.ai.IJson;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Resource-driven catalog of Eclipse-scope {@link JShellManualEntry} entries. Reads
 * {@code /eclipse-manual/index.json} from this bundle's classpath and serves each
 * entry's guide markdown lazily via {@link ManualResourceLoader}.
 * <p>
 * Lives in the shared {@code ui.common} bundle so the manuals are available in both
 * the EDT and the plain Eclipse plugin variants (the EDT-only {@code context} bundle
 * hosts the {@code edt}-scope catalog). The resource root is distinct from the
 * context bundle's {@code /manual} to avoid Require-Bundle resource shadowing.
 */
@Singleton
public class EclipseManualCatalog
    implements IJShellManualProvider
{
    private static final String INDEX_RESOURCE = "/eclipse-manual/index.json"; //$NON-NLS-1$

    private final ManualResourceLoader loader = new ManualResourceLoader(EclipseManualCatalog.class, "/eclipse-manual"); //$NON-NLS-1$
    private final IJson json;
    private volatile List<JShellManualEntry> entries;
    private volatile long indexMtime;

    @Inject
    public EclipseManualCatalog(IJson json)
    {
        Preconditions.checkNotNull(json);
        this.json = json;
        reload();
    }

    /**
     * Returns the manual entries, hot-reloading {@code index.json} when its underlying file
     * changed since the last load. In a packaged (jarred) bundle the modification time is
     * unavailable, so the entries are loaded once and cached (production behaviour). In a
     * self-hosted/dev launch the index resolves to a file, so edits are picked up without a
     * restart. Guide markdown bodies are always read lazily and are already live.
     * <p>
     * A missing or unparsable index never fails the caller (or DI): the catalog serves an
     * empty list and retries on the next call, so the index may appear later at runtime.
     */
    @Override
    public synchronized Collection<JShellManualEntry> getManualEntries()
    {
        long current = currentIndexMtime();
        if (entries == null || entries.isEmpty() || (current > 0 && current != indexMtime))
        {
            reload();
        }
        return Collections.unmodifiableList(entries);
    }

    private void reload()
    {
        try
        {
            this.entries = loadEntries();
        }
        catch (Exception e)
        {
            // Missing or broken index must not break the plugin — serve no entries and retry later.
            this.entries = List.of();
        }
        this.indexMtime = currentIndexMtime();
    }

    private long currentIndexMtime()
    {
        try
        {
            var url = EclipseManualCatalog.class.getResource(INDEX_RESOURCE);
            if (url == null)
            {
                return 0L;
            }
            return url.openConnection().getLastModified();
        }
        catch (Exception e)
        {
            return 0L;
        }
    }

    private List<JShellManualEntry> loadEntries()
    {
        var raw = readIndex();
        var manifests = json.deserialize(raw, Manifest[].class)
            .orElseThrow(() -> new IllegalStateException("Cannot parse " + INDEX_RESOURCE)); //$NON-NLS-1$
        var result = new ArrayList<JShellManualEntry>(manifests.length);
        for (var manifest : manifests)
        {
            result.add(toEntry(manifest));
        }
        return result;
    }

    private JShellManualEntry toEntry(Manifest manifest)
    {
        var bindings = manifest.bindings != null ? manifest.bindings : List.<String>of();
        var keywords = manifest.keywords != null ? manifest.keywords : List.<String>of();
        var vars = manifest.vars != null ? manifest.vars : Map.<String, String>of();
        var category = manifest.category != null ? manifest.category : JShellManualEntry.deriveCategory(manifest.id);
        return new JShellManualEntry(manifest.id, manifest.scope, category, manifest.title, manifest.summary,
            () -> loader.load(manifest.guide, vars), bindings, keywords);
    }

    private String readIndex()
    {
        try (InputStream stream = EclipseManualCatalog.class.getResourceAsStream(INDEX_RESOURCE))
        {
            if (stream == null)
            {
                throw new IllegalStateException("Manual index not found: " + INDEX_RESOURCE); //$NON-NLS-1$
            }
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
            {
                return reader.lines().collect(Collectors.joining("\n")); //$NON-NLS-1$
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot read " + INDEX_RESOURCE, e); //$NON-NLS-1$
        }
    }

    private static class Manifest
    {
        String id;
        String scope;
        String category;
        String title;
        String summary;
        String guide;
        List<String> bindings;
        List<String> keywords;
        HashMap<String, String> vars;
    }
}
