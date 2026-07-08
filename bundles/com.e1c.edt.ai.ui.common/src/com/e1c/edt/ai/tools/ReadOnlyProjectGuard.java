/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.IReadOnlyProjectGuard;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ReadOnlyProjectGuard
    implements IReadOnlyProjectGuard
{
    private final IEditingSupport editingSupport;

    @Inject
    public ReadOnlyProjectGuard(IEditingSupport editingSupport)
    {
        Preconditions.checkNotNull(editingSupport);

        this.editingSupport = editingSupport;
    }

    @SuppressWarnings("nls")
    @Override
    public void checkWritable(IProject project)
    {
        if (editingSupport.isReadOnly(project))
        {
            throw new ToolException("The project \"" + project.getName()
                + "\" is read-only: this 1C configuration is on full vendor support "
                + "and editing is not allowed. Creating, editing, or deleting its files is not allowed. "
                + "Do not retry and do not try to bypass this via JShell, EDT commands, or git. "
                + "Inform the user that changes require enabling editing for the configuration first "
                + "(changing the support mode in the configuration support settings), "
                + "or that the change should be implemented in an extension project.",
                ToolErrorType.USER_VISIBLE);
        }
    }
}
