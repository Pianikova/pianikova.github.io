/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.bm.core.IBmEngine;
import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com.google.common.base.Preconditions;

public class BmRoot
{
    private final String path;
    private final URI uri;
    private final IBmModel model;
    private final IProject project;
    private final IDtProject dtProject;
    private final IBmEngine engine;
    private final IBmObject bmObject;
    private final IProjectFileSystemSupportProvider projectFileSystemSupportProvider;

    public BmRoot(String path, URI uri, IProject project, IBmModel model, IDtProject dtProject, IBmEngine engine,
        IBmObject bmObject, IProjectFileSystemSupportProvider projectFileSystemSupportProvider)
    {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(uri);
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(model);
        Preconditions.checkNotNull(dtProject);
        Preconditions.checkNotNull(engine);
        Preconditions.checkNotNull(bmObject);
        Preconditions.checkNotNull(projectFileSystemSupportProvider);
        this.path = path;
        this.uri = uri;
        this.project = project;
        this.model = model;
        this.dtProject = dtProject;
        this.engine = engine;
        this.bmObject = bmObject;
        this.projectFileSystemSupportProvider = projectFileSystemSupportProvider;
    }

    public String getPath()
    {
        return path;
    }

    public URI getUri()
    {
        return uri;
    }

    public IProject getProject()
    {
        return project;
    }

    public IBmModel getModel()
    {
        return model;
    }

    public IDtProject getDtProject()
    {
        return dtProject;
    }

    public IBmEngine getEngine()
    {
        return engine;
    }

    public IBmObject getBmObject()
    {
        return bmObject;
    }

    public Optional<IFile> getFile(EObject obj)
    {
        return Optional.ofNullable(
            projectFileSystemSupportProvider.getProjectFileSystemSupport(getDtProject()).getFile(obj));
    }
}
