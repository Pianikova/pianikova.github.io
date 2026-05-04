/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools;

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
import com.e1c.edt.ai.tools.IJShellManualProvider;
import com.e1c.edt.ai.tools.JShellManualEntry;
import com.e1c.edt.ai.tools.ManualResourceLoader;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Resource-driven catalog of {@link JShellManualEntry} entries. Reads
 * {@code /manual/index.json} from the bundle classpath and serves each entry's
 * guide markdown lazily via {@link ManualResourceLoader}.
 * <p>
 * Migration target for the manual half of {@link MetadataBindingProvider}: as a
 * scenario gets externalized into {@code resources/manual/}, its corresponding
 * Java {@code build*Workflow()} method and {@link MetadataBindingProvider#getManualEntries()}
 * registration are removed; the new entry surfaces through this class instead.
 */
@Singleton
public class MetadataManualCatalog
    implements IJShellManualProvider
{
    private static final String INDEX_RESOURCE = "/manual/index.json"; //$NON-NLS-1$

    private final ManualResourceLoader loader = new ManualResourceLoader(MetadataManualCatalog.class, "/manual"); //$NON-NLS-1$
    private final IJson json;
    private final List<JShellManualEntry> entries;

    @Inject
    public MetadataManualCatalog(IJson json)
    {
        Preconditions.checkNotNull(json);
        this.json = json;
        this.entries = loadEntries();
    }

    @Override
    public Collection<JShellManualEntry> getManualEntries()
    {
        return Collections.unmodifiableList(entries);
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
        var guidePath = manifest.guide;
        var category = manifest.category != null ? manifest.category : JShellManualEntry.deriveCategory(manifest.id);
        return new JShellManualEntry(manifest.id, manifest.scope, category, manifest.title, manifest.summary,
            () -> loader.load(guidePath, vars), bindings, keywords);
    }

    private String readIndex()
    {
        try (InputStream stream = MetadataManualCatalog.class.getResourceAsStream(INDEX_RESOURCE))
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
