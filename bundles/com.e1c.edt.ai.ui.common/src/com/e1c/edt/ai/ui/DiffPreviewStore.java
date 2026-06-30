/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.google.inject.Singleton;

/**
 * In-memory {@link IDiffPreviewStore} backed by a bounded, access-ordered {@link LinkedHashMap}
 * (LRU). The cap keeps memory bounded over a long chat session; evicted entries simply mean the
 * corresponding {@code edt-diff://} link becomes inert (re-rendering the message repopulates it).
 */
@Singleton
public class DiffPreviewStore
    implements IDiffPreviewStore
{
    private static final int MAX_ENTRIES = 200;

    private final Map<String, DiffPreview> entries =
        Collections.synchronizedMap(new LinkedHashMap<String, DiffPreview>(16, 0.75f, true)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, DiffPreview> eldest)
            {
                return size() > MAX_ENTRIES;
            }
        });

    @Override
    public void put(String token, DiffPreview preview)
    {
        if (token == null || token.isBlank() || preview == null)
        {
            return;
        }
        entries.put(token, preview);
    }

    @Override
    public Optional<DiffPreview> get(String token)
    {
        if (token == null || token.isBlank())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(token));
    }
}
