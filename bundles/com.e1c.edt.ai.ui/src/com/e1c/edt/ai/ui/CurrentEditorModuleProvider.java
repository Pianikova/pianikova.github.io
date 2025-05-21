/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.jface.text.IDocument;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.context.IModuleProvider;
import com.e1c.edt.ai.context.ModuleInfo;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

class CurrentEditorModuleProvider
    implements IModuleProvider
{
    private final IModuleProvider baseResourceSetProvider;

    @Inject
    public CurrentEditorModuleProvider(@Named("BaseModuleProvider") IModuleProvider baseResourceSetProvider)
    {
        Preconditions.checkNotNull(baseResourceSetProvider);
        this.baseResourceSetProvider = baseResourceSetProvider;
    }

    @Override
    public Optional<ModuleInfo> getModule(IDocument document, String filePath, ICancellationToken cancellationToken)
    {
        var optionalModuleInfo = baseResourceSetProvider.getModuleInfo(document, cancellationToken);
        if (optionalModuleInfo.isEmpty())
        {
            return baseResourceSetProvider.getModule(document, filePath, cancellationToken);
        }

        var moduleInfo = optionalModuleInfo.get();
        var moduleFilePath = moduleInfo.getFilePath();
        if (!filePath.equals(moduleFilePath))
        {
            return baseResourceSetProvider.getModule(document, moduleFilePath, cancellationToken);
        }

        return Optional.of(moduleInfo);
    }

    @Override
    public Optional<ModuleInfo> getModuleInfo(IDocument document, ICancellationToken cancellationToken)
    {
        return baseResourceSetProvider.getModuleInfo(document, cancellationToken);
    }
}
