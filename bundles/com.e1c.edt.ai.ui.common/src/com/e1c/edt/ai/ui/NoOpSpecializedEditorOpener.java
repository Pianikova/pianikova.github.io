/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Default no-op {@link ISpecializedEditorOpener}: returns {@code null} so the caller falls back
 * to the generic open path. Used in non-EDT environments.
 */
public class NoOpSpecializedEditorOpener
    implements ISpecializedEditorOpener
{
    @Override
    public IEditorPart openInSpecializedEditor(IWorkbenchPage page, IFile file)
    {
        return null;
    }
}
