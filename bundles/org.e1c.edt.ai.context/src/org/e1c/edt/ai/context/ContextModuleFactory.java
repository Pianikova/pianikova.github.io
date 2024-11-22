/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.eclipse.core.runtime.Plugin;

import com.google.inject.util.Modules;
import com.google.inject.util.Modules.OverriddenModuleBuilder;

public class ContextModuleFactory
{
    public static OverriddenModuleBuilder create(Plugin plugin)
    {
        return Modules.override(new ContextModule(plugin));
    }
}
