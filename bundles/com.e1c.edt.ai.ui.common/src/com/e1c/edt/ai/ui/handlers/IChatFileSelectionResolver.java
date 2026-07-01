/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import java.util.List;

import com.e1c.edt.ai.IFileDocument;

/**
 * Resolves a workbench selection (files, folders, 1C metadata objects) into the list of
 * {@link IFileDocument} to attach to the chat. Folders are expanded recursively.
 */
public interface IChatFileSelectionResolver
{
    List<IFileDocument> resolve(List<?> targets);

    /**
     * Lightweight check whether the selection contains at least one item that {@link #resolve} could
     * turn into a chat attachment (a file, a folder, or a resolvable metadata entity). Does not read
     * file contents, so it is cheap enough to call from drag-over feedback. Group/collection
     * navigator nodes (e.g. "Documents", "Catalogs") resolve to nothing and therefore return
     * {@code false}.
     *
     * @param targets the dragged/selected elements
     * @return {@code true} if a drop would add at least one attachment
     */
    boolean canResolve(List<?> targets);
}
