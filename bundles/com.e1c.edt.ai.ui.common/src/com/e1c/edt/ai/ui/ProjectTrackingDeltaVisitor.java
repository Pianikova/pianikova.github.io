/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class ProjectTrackingDeltaVisitor
    implements IProjectTrackingDeltaVisitor
{
    private static final Set<String> EXTENSIONS = Set.of("bsl", "mdo", "form"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
        var resource = delta.getResource();
        if (resource == null)
        {
            return true;
        }

        // Keep descending into containers; only individual files of the tracked kinds are synced.
        if (resource.getType() != IResource.FILE)
        {
            return true;
        }

        var ext = resource.getFileExtension();
        if (ext == null || !EXTENSIONS.contains(ext.toLowerCase()))
        {
            return false;
        }

        var kind = delta.getKind();
        var addedOrRemoved = (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED);
        // A CHANGED delta fires for marker/derived-flag updates too; only react to actual content changes.
        var contentChanged =
            kind == IResourceDelta.CHANGED && (delta.getFlags() & IResourceDelta.CONTENT) != 0;
        if (addedOrRemoved || contentChanged)
        {
            track(delta);
        }

        // A file has no children to visit.
        return false;
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

        var projectId = new ProjectId(project);
        var path = resource.getFullPath().makeRelative().toPortableString();
        log.trace(TracingSources.SYNC, "ResourceListener", () -> path + " was updated in project " + project.getName());
        var ctx = new AIContext(projectId, path, null);
        globalContextTracker.track(ctx);
    }
}
