/**
 *
 */
package com.e1c.edt.ai;

import org.eclipse.core.resources.IFile;

public interface IEditingSupport
{
    boolean canEdit(IFile file);

    boolean canCreate(IFile file);

    boolean canDelete(IFile file);
}
