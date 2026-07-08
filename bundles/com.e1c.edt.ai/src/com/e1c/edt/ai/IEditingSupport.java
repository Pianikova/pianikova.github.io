/**
 *
 */
package com.e1c.edt.ai;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public interface IEditingSupport
{
    boolean canEdit(IFile file);

    boolean canCreate(IFile file);

    boolean canDelete(IFile file);

    /**
     * Returns {@code true} when the whole project is read-only for AI tools.
     * A 1C configuration project is read-only when its Configuration is on full vendor
     * support and editing is not allowed. Extension projects are never read-only by this
     * rule. Indeterminate states (project closed, V8 model not loaded yet, non-1C
     * project) return {@code false}.
     */
    boolean isReadOnly(IProject project);
}
