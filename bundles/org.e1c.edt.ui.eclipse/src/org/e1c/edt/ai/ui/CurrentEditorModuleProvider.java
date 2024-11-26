/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.context.IModuleProvider;
import org.e1c.edt.ai.context.ModuleInfo;
import org.e1c.edt.ai.ui.AIUIModule.BaseModuleProvider;
import org.eclipse.jface.text.IDocument;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class CurrentEditorModuleProvider
    implements IModuleProvider
{
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final IModuleProvider baseResourceSetProvider;

    @Inject
    public CurrentEditorModuleProvider(IUI ui, IDispatcher dispatcher,
        @BaseModuleProvider IModuleProvider baseResourceSetProvider)
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
            .flatMap(sourceViewer -> baseResourceSetProvider.getModuleInfo(sourceViewer.getDocument()));

        if (optionalModuleInfo.isEmpty())
        {
            return baseResourceSetProvider.getModule(filePath, cancellationToken);
        }

        var moduleInfo = optionalModuleInfo.get();
        var moduleFilePath = moduleInfo.getFilePath();
        if (!filePath.equals(moduleFilePath))
        {
            return baseResourceSetProvider.getModule(filePath, cancellationToken);
        }

        return Optional.of(moduleInfo);
    }

    @Override
    public Optional<ModuleInfo> getModuleInfo(IDocument document)
    {
        return baseResourceSetProvider.getModuleInfo(document);
    }
}
