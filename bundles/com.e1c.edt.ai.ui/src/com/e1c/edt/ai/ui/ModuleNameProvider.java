/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.core.resources.IProject;

import com._1c.g5.v8.dt.ui.util.Labeler;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.context.IModuleProvider;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Provides module name.
 *
 * @author Bogdan Sushkov
 *
 */
public class ModuleNameProvider
    implements IModuleNameProvider
{
    private final IModuleProvider moduleProvider;

    @Inject
    public ModuleNameProvider(IModuleProvider moduleProvider)
    {
        this.moduleProvider = Preconditions.checkNotNull(moduleProvider);
    }

    @Override
    public Optional<String> getModuleName(String path)
    {
        return moduleProvider.getModule(null, path, CancellationTokens.NONE)
            .map(module -> module.getModule())
            .map(module -> Labeler.path(module, '→')
                .skipCommonNode()
                .filter(candidate -> !(candidate instanceof IProject)))
            .map(moduleLabel -> moduleLabel.stopAfter(IProject.class))
            .map(labeler -> labeler.label());
    }

}
