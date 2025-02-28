/**
 * Copyright (C) 2025, 1C-Soft LLC
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.context.ContextModuleFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class Activator
    extends BaseActivator
{
    @Override
    protected Injector createInjector()
    {
        var mergedModule = ContextModuleFactory.create(this).with(new AIUICommonModule(), new AIUIModule(this));
        return Guice.createInjector(mergedModule);
    }

    @Override
    public String getPluginId()
    {
        return "com.e1c.edt.ai.ui"; //$NON-NLS-1$
    }
}
