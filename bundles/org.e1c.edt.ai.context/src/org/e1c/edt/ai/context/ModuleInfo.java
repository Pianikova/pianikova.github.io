/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.function.Supplier;

import com._1c.g5.v8.dt.bsl.model.Module;
import com.google.common.base.Preconditions;

public class ModuleInfo
{
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
        return module.eResource().getURI().path();
    }
}
