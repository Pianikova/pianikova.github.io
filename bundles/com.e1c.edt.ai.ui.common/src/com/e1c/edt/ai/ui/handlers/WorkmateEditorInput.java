/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.ui.ide.FileStoreEditorInput;

/**
 * File-store editor input with a descriptive tab title (e.g. {@code git-commit · SKILL.md [project]})
 * and the full path as the tab tooltip, so it is clear which skill/scope an opened file belongs to.
 * Equality is inherited from {@link FileStoreEditorInput} (by URI) so re-opening focuses the existing
 * editor.
 */
class WorkmateEditorInput
    extends FileStoreEditorInput
{
    private final String title;
    private final String tooltip;

    WorkmateEditorInput(IFileStore fileStore, String title, String tooltip)
    {
        super(fileStore);
        this.title = title;
        this.tooltip = tooltip;
    }

    @Override
    public String getName()
    {
        return title;
    }

    @Override
    public String getToolTipText()
    {
        return tooltip;
    }
}
