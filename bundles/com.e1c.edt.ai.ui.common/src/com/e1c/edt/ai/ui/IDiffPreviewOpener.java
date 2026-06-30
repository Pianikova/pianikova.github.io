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
     * Opens a read-only compare editor for the diff preview registered under the given token, with
     * focus, reusing an already-open editor for the same token if present. No-op if the token is
     * unknown. Must be called on the UI thread. Used for explicit user clicks on a diff link.
     *
     * @param token the diff-preview token
     */
    void openDiff(String token);

    /**
     * Automatically opens the compare editor for the given token without taking focus, at most once
     * per token per session (so repeated RENDER passes and re-renders do not re-open or steal focus,
     * and a tab the user closed is not re-opened). No-op if the token is unknown. Must be called on
     * the UI thread.
     *
     * @param token the diff-preview token
     */
    void autoOpenDiff(String token);

    /**
     * Closes the compare editor previously opened for the given token, if it is still open. No-op
     * otherwise. Must be called on the UI thread.
     *
     * @param token the diff-preview token
     */
    void closeDiff(String token);
}
