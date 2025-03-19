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
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final IModuleProvider baseResourceSetProvider;

    @Inject
    public CurrentEditorModuleProvider(IUI ui, IDispatcher dispatcher,
        @Named("BaseModuleProvider") IModuleProvider baseResourceSetProvider)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(baseResourceSetProvider);
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.baseResourceSetProvider = baseResourceSetProvider;
    }

    @Override
    public Optional<ModuleInfo> getModule(String filePath, ICancellationToken cancellationToken)
    {
        var optionalModuleInfo = dispatcher.dispatch(() -> ui.getTextWidget()
            .flatMap(textWidget -> ui.getSourceViewer(textWidget)))
            .orElse(null)
            .flatMap(
                sourceViewer -> baseResourceSetProvider.getModuleInfo(sourceViewer.getDocument(), cancellationToken));

        if (optionalModuleInfo.isEmpty())
        {
            return baseResourceSetProvider.getModule(filePath, cancellationToken);
        }

        var moduleInfo = optionalModuleInfo.get();
        var moduleFilePath = moduleInfo.getFilePath();
        if (!filePath.equals(moduleFilePath))
        {
            return baseResourceSetProvider.getModule(moduleFilePath, cancellationToken);
        }

        return Optional.of(moduleInfo);
    }

    @Override
    public Optional<ModuleInfo> getModuleInfo(IDocument document, ICancellationToken cancellationToken)
    {
        return baseResourceSetProvider.getModuleInfo(document, cancellationToken);
    }
}
