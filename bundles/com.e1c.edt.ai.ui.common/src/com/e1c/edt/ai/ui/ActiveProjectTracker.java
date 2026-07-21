/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ActiveProjectTracker
    implements IInitializable, IResourceChangeListener, IStateListener
{
    private final ILog log;
    private final IGlobalContextTracker globalContextTracker;
    private final ISettings settings;
    private final IStateService stateService;
    private final IGlobalContextStateStore stateStore;

    @Inject
    public ActiveProjectTracker(ILog log, IGlobalContextTracker globalContextTracker, ISettings settings,
        IStateService stateService, IGlobalContextStateStore stateStore)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(globalContextTracker);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(stateService);
        Preconditions.checkNotNull(stateStore);
        this.log = log;
        this.globalContextTracker = globalContextTracker;
        this.settings = settings;
        this.stateService = stateService;
        this.stateStore = stateStore;
    }

    @Override
    public void initialize()
    {
        stateService.addListener(this);
        trackAllActiveProjects();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
    }

    private void trackAllActiveProjects()
    {
        if (!settings.isEnabled())
        {
            return;
        }

        var workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        var projects = workspaceRoot.getProjects();

        for (IProject project : projects)
        {
            if (project.isAccessible())
            {
                globalContextTracker.track(project);
            }
        }
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        if (event.getType() != IResourceChangeEvent.POST_CHANGE)
        {
            return;
        }

        if (!settings.isEnabled())
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
            rootDelta.accept(delta -> {
                if (isProjectAdded(delta))
                {
                    trackProject(delta);
                }
                else if (isProjectRemoved(delta))
                {
                    deleteProjectState(delta);
                }
                return true;
            });
        }
        catch (CoreException error)
        {
            log.logError(error);
        }
    }

    private boolean isProjectAdded(IResourceDelta delta)
    {
        var kind = delta.getKind();
        return (kind & IResourceDelta.ADDED) != 0;
    }

    private boolean isProjectRemoved(IResourceDelta delta)
    {
        // REMOVED is a deletion from the workspace. A project close is a CHANGED+OPEN delta, so this does not fire
        // on close (we keep the state for a reopen).
        return (delta.getKind() & IResourceDelta.REMOVED) != 0;
    }

    private void deleteProjectState(IResourceDelta delta)
    {
        var resource = delta.getResource();
        if (resource == null || resource.getType() != IResource.PROJECT)
        {
            return;
        }

        stateStore.delete((IProject)resource);
    }

    private void trackProject(IResourceDelta delta)
    {
        var resource = delta.getResource();
        if (resource == null || resource.getType() != IResource.PROJECT)
        {
            return;
        }

        var project = (IProject)resource;
        if (project.isAccessible())
        {
            globalContextTracker.track(project);
        }
    }

    @Override
    public void onServiceStateChange(ServiceState serviceState)
    {
        if (serviceState == ServiceState.SETTINGS_CHANGED)
        {
            trackAllActiveProjects();
        }
    }

    @Override
    public void onActionStateChange(ActionState actionState)
    {
        // Do nothing
    }
}
