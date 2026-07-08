/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai;

import org.eclipse.core.resources.IProject;

/**
 * Refuses file mutations in projects whose 1C configuration is read-only
 * (on full vendor support, editing not allowed).
 */
public interface IReadOnlyProjectGuard
{
    /**
     * Throws a {@link ToolException} with {@link ToolErrorType#USER_VISIBLE} when the
     * project is read-only (see {@link IEditingSupport#isReadOnly(IProject)}); does
     * nothing otherwise.
     */
    void checkWritable(IProject project);
}
