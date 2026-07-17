/**
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectProvider;
import com.e1c.edt.ai.TracingSources;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class CurrentProjectResolver
    implements ICurrentProjectResolver
{
    private static final String TRACE_TOPIC = "Project resolver"; //$NON-NLS-1$

    private final IProjectProvider projectProvider;
    private final IDispatcher dispatcher;
    private final ILog log;

    @Inject
    CurrentProjectResolver(IProjectProvider projectProvider, IDispatcher dispatcher, ILog log)
    {
        this.projectProvider = Preconditions.checkNotNull(projectProvider);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.log = Preconditions.checkNotNull(log);
    }

    @Override
    public Optional<IProject> resolve(String path)
    {
        return projectProvider.getProject(path).or(this::resolve);
    }

    @Override
    public Optional<IProject> resolve()
    {
        var project = dispatcher.dispatch(this::resolveOnUiThread).flatMap(value -> value);
        if (project.isEmpty())
        {
            log.trace(TracingSources.COMMON, TRACE_TOPIC,
                () -> "Project resolution failed: source=current-context"); //$NON-NLS-1$
        }
        return project;
    }

    private Optional<IProject> resolveOnUiThread()
    {
        var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        var page = window == null ? null : window.getActivePage();
        if (page != null)
        {
            var editor = page.getActiveEditor();
            var input = editor == null ? null : editor.getEditorInput();
            var file = input == null ? null : input.getAdapter(IFile.class);
            if (file != null)
            {
                var project = Optional.of(file.getProject()).filter(IProject::isAccessible);
                if (project.isPresent())
                {
                    return traced(project, "editor"); //$NON-NLS-1$
                }
            }

            var selection = page.getSelection();
            if (selection instanceof IStructuredSelection)
            {
                var project = project(((IStructuredSelection)selection).getFirstElement());
                if (project.isPresent())
                {
                    return traced(project, "selection"); //$NON-NLS-1$
                }
            }
        }

        var projects = Arrays.stream(ResourcesPlugin.getWorkspace().getRoot().getProjects())
            .filter(IProject::isAccessible)
            .limit(2)
            .collect(Collectors.toList());
        return projects.size() == 1 ? traced(Optional.of(projects.get(0)), "single-project") //$NON-NLS-1$
            : Optional.empty();
    }

    private Optional<IProject> traced(Optional<IProject> project, String source)
    {
        project.ifPresent(value -> log.trace(TracingSources.COMMON, TRACE_TOPIC,
            () -> "Project resolved: source=" + source + ", project=" + value.getName())); //$NON-NLS-1$ //$NON-NLS-2$
        return project;
    }

    private Optional<IProject> project(Object value)
    {
        if (value instanceof IResource)
        {
            return Optional.of(((IResource)value).getProject()).filter(IProject::isAccessible);
        }
        if (value instanceof IAdaptable)
        {
            var resource = ((IAdaptable)value).getAdapter(IResource.class);
            return resource == null ? Optional.empty()
                : Optional.of(resource.getProject()).filter(IProject::isAccessible);
        }
        return Optional.empty();
    }
}
