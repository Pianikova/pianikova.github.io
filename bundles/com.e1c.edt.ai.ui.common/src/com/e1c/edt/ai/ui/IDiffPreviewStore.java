/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

/**
 * Session-scoped registry of {@link DiffPreview} payloads keyed by an opaque token (typically the
 * tool-call id). Populated by the {@code Edit} tool at RENDER time and read by the diff-preview link
 * handler when the user clicks an {@code edt-diff://} link.
 */
public interface IDiffPreviewStore
{
    /**
     * Stores (or replaces) the diff preview under the given token.
     *
     * @param token the diff-preview token, never {@code null}
     * @param preview the payload, never {@code null}
     */
    void put(String token, DiffPreview preview);

    /**
     * @param token the diff-preview token
     * @return the stored preview, or empty if unknown/expired
     */
    Optional<DiffPreview> get(String token);
}
