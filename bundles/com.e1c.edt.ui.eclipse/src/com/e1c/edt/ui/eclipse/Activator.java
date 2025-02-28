/**
 * Copyright (C) 2025, 1C-Soft LLC
 */
package com.e1c.edt.ui.eclipse;

import com.e1c.edt.ai.ui.AIUICommonModule;
import com.e1c.edt.ai.ui.BaseActivator;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class Activator
    extends BaseActivator
{
    @Override
    protected Injector createInjector()
    {
        var mergedModule = Modules.override(new AIUICommonModule()).with(new AIUIModule(this));
        return Guice.createInjector(mergedModule);
    }

    @Override
    public String getPluginId()
    {
        return "com.e1c.edt.ui.eclipse"; //$NON-NLS-1$
    }
}
