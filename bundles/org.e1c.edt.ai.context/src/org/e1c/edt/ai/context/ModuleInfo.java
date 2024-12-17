/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.eclipse.emf.common.util.URI;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.google.common.base.Preconditions;

public class ModuleInfo
{
    private final static URI BasePath = URI.createURI("platform:/resource/"); //$NON-NLS-1$
    private final Module module;
    private final String filePath;

    public ModuleInfo(Module module, String filePath)
    {
        Preconditions.checkNotNull(module);
        this.module = module;
        this.filePath = filePath;
    }

    public Module getModule()
    {
        return module;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public String getFilePath2()
    {
        var uri = module.eResource().getURI();
        return uri.deresolve(BasePath).path();
    }
}
