/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

import com._1c.g5.v8.dt.ui.util.OpenHelper;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * EDT-aware {@link ISpecializedEditorOpener} that delegates to {@link OpenHelper}.
 * <p>
 * {@code OpenHelper.openEditor(IFile, ISelection)} resolves the workspace file to a 1C metadata
 * model object via a platform-resource URI and opens the DT editor registered for that object
 * (MD object editor, form editor, BSL module editor). For files with no model mapping it falls
 * back to {@code IDE.getEditorDescriptor(file)}.
 */
public class EdtSpecializedEditorOpener
    implements ISpecializedEditorOpener
{
    private final ILog log;

    @Inject
    public EdtSpecializedEditorOpener(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    @SuppressWarnings("nls")
    public IEditorPart openInSpecializedEditor(IWorkbenchPage page, IFile file)
    {
        try
        {
            return new OpenHelper(page).openEditor(file, null);
        }
        catch (Exception e)
        {
            log.logError("EdtSpecializedEditorOpener failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }
}
