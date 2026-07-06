/**
 *
 */
package com.e1c.edt.ui.eclipse;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.IEditingSupport;

public class EditingSupport
    implements IEditingSupport
{
    @Override
    public boolean canEdit(IFile file)
    {
        return true;
    }

    @Override
    public boolean canCreate(IFile file)
    {
        return true;
    }

    @Override
    public boolean canDelete(IFile file)
    {
        return true;
    }

    @Override
    public boolean isReadOnly(IProject project)
    {
        return false;
    }

}
