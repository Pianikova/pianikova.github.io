/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

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
