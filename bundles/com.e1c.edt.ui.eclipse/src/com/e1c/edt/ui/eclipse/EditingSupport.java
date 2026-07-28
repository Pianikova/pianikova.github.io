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

    @Override
    public boolean canEdit(Object object)
    {
        // No 1C model here, so there are no vendor-support rules to consult.
        return true;
    }

    @Override
    public boolean canDelete(Object object)
    {
        return true;
    }

}
