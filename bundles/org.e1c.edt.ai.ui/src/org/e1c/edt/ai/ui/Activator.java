/**
 * Copyright (C) 2023, 1C-Soft LLC
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.context.ContextModuleFactory;

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
}
