/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.eclipse.core.resources.IProject;

import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.bsl.model.Module;
import com.google.common.base.Preconditions;

public class ModuleInfo
{
    private final IProject project;
    private final IBmModel bmModel;
    private final Module module;

    public ModuleInfo(IProject project, IBmModel bmModel, Module module)
    {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(bmModel);
        Preconditions.checkNotNull(module);
        this.project = project;
        this.bmModel = bmModel;
        this.module = module;
    }

    public IProject getProject()
    {
        return project;
    }

    public IBmModel getBmModel()
    {
        return bmModel;
    }

    public Module getModule()
    {
        return module;
    }
}
