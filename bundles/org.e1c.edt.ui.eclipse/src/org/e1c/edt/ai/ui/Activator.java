/**
 * Copyright (C) 2023, 1C-Soft LLC
 */
package org.e1c.edt.ai.ui;

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
}
