/**
 *
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;

public class CommitMessage
{
    private final ProjectId projectId;
    private final String uuid;
    private final String message;

    public CommitMessage(ProjectId projectId, String uuid, String message)
    {
        Preconditions.checkNotNull(projectId);
        Preconditions.checkNotNull(uuid);
        Preconditions.checkNotNull(message);
        this.projectId = projectId;
        this.uuid = uuid;
        this.message = message;
    }

    public ProjectId getProjectId()
    {
        return projectId;
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
