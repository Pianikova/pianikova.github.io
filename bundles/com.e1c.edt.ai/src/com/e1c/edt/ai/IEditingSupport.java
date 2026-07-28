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

    /**
     * Returns {@code true} when this metadata object may be modified.
     * <p>
     * This is the per-object vendor-support rule, which {@link #isReadOnly(IProject)} does not cover:
     * a configuration can be on support with editing allowed ("editable with preservation"), so the
     * project is writable while individual adopted objects still must not change. Answers the same
     * question the EDT editors ask, so support rules are never reimplemented here.
     *
     * @param object metadata object, may be {@code null}
     * @return {@code true} when the object may be modified, also for a {@code null} or unknown object
     */
    boolean canEdit(Object object);

    /**
     * Returns {@code true} when this metadata object may be deleted. See {@link #canEdit(Object)}.
     *
     * @param object metadata object, may be {@code null}
     * @return {@code true} when the object may be deleted, also for a {@code null} or unknown object
     */
    boolean canDelete(Object object);
}
