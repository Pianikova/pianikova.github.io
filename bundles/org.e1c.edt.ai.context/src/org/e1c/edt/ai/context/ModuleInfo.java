/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.function.Supplier;

import org.eclipse.emf.common.util.URI;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.google.common.base.Preconditions;

public class ModuleInfo
{
    private final static URI BasePath = URI.createURI("platform:/resource/"); //$NON-NLS-1$
    private final Module module;
    private final Supplier<String> contentSupplier;

    public ModuleInfo(Module module, Supplier<String> contentSupplier)
    {
        Preconditions.checkNotNull(module);
        this.module = module;
        this.contentSupplier = contentSupplier;
    }

    public Module getModule()
    {
        return module;
    }

    public String readContent()
    {
        if (contentSupplier == null)
        {
            return null;
        }

        return contentSupplier.get();
    }

    public String getFilePath()
    {
        var uri = module.eResource().getURI();
        return uri.deresolve(BasePath).path();
    }
}
