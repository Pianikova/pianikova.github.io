/**
 *
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ResourceListener
    implements IInitializable, IResourceChangeListener
{
    private final ILog log;
    private final IProjectTrackingDeltaVisitor visitor;

    @Inject
    public ResourceListener(ILog log, IProjectTrackingDeltaVisitor visitor)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(visitor);
        this.log = log;
        this.visitor = visitor;
    }

    @Override
    public void initialize()
    {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
        {
            return;
        }

        IResourceDelta rootDelta = event.getDelta();
        if (rootDelta == null)
        {
            return;
        }

        try
        {
            rootDelta.accept(visitor);
        }
        catch (CoreException error)
        {
            log.logError(error);
        }
    }
}
