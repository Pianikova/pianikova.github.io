/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

/**
 * Opens a dedicated Eclipse compare view for a stored {@link DiffPreview}, identified by a token
 * carried in an {@code edt-diff://} link.
 */
public interface IDiffPreviewOpener
{
    /**
     * Opens a read-only compare editor for the diff preview registered under the given token. No-op
     * if the token is unknown. Must be called on the UI thread.
     *
     * @param token the diff-preview token
     */
    void openDiff(String token);
}
