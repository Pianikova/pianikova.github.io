/**
 *
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IProject;
import com.google.common.base.Preconditions;

public class CommitMessage
{
    private final IProject project;
    private final String uuid;
    private final String message;

    public CommitMessage(IProject project, String uuid, String message)
    {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(uuid);
        Preconditions.checkNotNull(message);
        this.project = project;
        this.uuid = uuid;
        this.message = message;
    }

    public IProject getProject()
    {
        return project;
    }

    public String getUuid()
    {
        return uuid;
    }

    public String getMessage()
    {
        return message;
    }
}
