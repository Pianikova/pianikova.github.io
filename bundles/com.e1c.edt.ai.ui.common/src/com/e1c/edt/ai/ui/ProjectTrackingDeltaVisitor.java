/**
 *
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class ProjectTrackingDeltaVisitor
    implements IProjectTrackingDeltaVisitor
{
    private final ILog log;
    private final IGlobalContextTracker globalContextTracker;

    @Inject
    public ProjectTrackingDeltaVisitor(ILog log, IGlobalContextTracker globalContextTracker)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(globalContextTracker);
        this.log = log;
        this.globalContextTracker = globalContextTracker;
    }

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException
    {
        if ((delta.getKind() & (IResourceDelta.REMOVED | IResourceDelta.MOVED_TO)) != 0)
        {
            track(delta);
        }

        return true;
    }

    @SuppressWarnings("nls")
    private void track(IResourceDelta delta)
    {
        var resource = delta.getResource();
        if (resource == null)
        {
            return;
        }

        var project = resource.getProject();
        if (project == null)
        {
            return;
        }

        var projectId = new ProjectId(project.getName(), project);
        var path = resource.getFullPath().makeRelative().toPortableString();
        log.trace("ResourceListener", () -> path + " was updated in project " + project.getName());
        var ctx = new AIContext(projectId, path, null);
        globalContextTracker.track(ctx);
    }
}
