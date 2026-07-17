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

import com.e1c.edt.ai.IProjectProvider;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class CurrentProjectResolver
    implements ICurrentProjectResolver
{
    private final IProjectProvider projectProvider;

    @Inject
    CurrentProjectResolver(IProjectProvider projectProvider)
    {
        this.projectProvider = Preconditions.checkNotNull(projectProvider);
    }

    @Override
    public Optional<IProject> resolve(String path)
    {
        return projectProvider.getProject(path).or(this::resolve);
    }

    @Override
    public Optional<IProject> resolve()
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
                return Optional.of(file.getProject()).filter(IProject::isAccessible);
            }

            var selection = page.getSelection();
            if (selection instanceof IStructuredSelection)
            {
                var project = project(((IStructuredSelection)selection).getFirstElement());
                if (project.isPresent())
                {
                    return project;
                }
            }
        }

        var projects = Arrays.stream(ResourcesPlugin.getWorkspace().getRoot().getProjects())
            .filter(IProject::isAccessible)
            .limit(2)
            .collect(Collectors.toList());
        return projects.size() == 1 ? Optional.of(projects.get(0)) : Optional.empty();
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
