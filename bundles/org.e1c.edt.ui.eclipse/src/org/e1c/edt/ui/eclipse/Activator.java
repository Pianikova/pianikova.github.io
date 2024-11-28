/**
 * Copyright (C) 2023, 1C-Soft LLC
 */
package org.e1c.edt.ui.eclipse;

import org.e1c.edt.ai.ui.AIUICommonModule;
import org.e1c.edt.ai.ui.BaseActivator;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class Activator
    extends BaseActivator
{
    @Override
    protected Injector createInjector()
    {
        var mergedModule = Modules.override(new AIUIModule(this)).with(new AIUICommonModule());
        return Guice.createInjector(mergedModule);
    }

    @Override
    public String getPluginId()
    {
        return "org.e1c.edt.ui.eclipse"; //$NON-NLS-1$
    }
}
