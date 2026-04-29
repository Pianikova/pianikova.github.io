/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

/**
 * SPI for opening a workspace file in a domain-specific editor.
 * <p>
 * Default implementation in {@code com.e1c.edt.ai.ui.common} is a no-op so that this bundle stays
 * independent from EDT (1C) APIs. EDT-aware bundles override the binding to provide an
 * implementation that resolves the file to a metadata model object and opens the proper DT editor.
 */
public interface ISpecializedEditorOpener
{
    /**
     * Tries to open the given workspace file in a specialized editor.
     *
     * @param page the active workbench page, never {@code null}
     * @param file the workspace file to open, never {@code null}
     * @return the opened editor part, or {@code null} if no specialized editor is applicable —
     *         the caller should then fall back to a generic open mechanism
     */
    IEditorPart openInSpecializedEditor(IWorkbenchPage page, IFile file);
}
