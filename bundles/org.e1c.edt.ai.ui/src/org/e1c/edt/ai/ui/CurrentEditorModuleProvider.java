/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.context.IModuleProvider;
import org.e1c.edt.ai.context.ModuleInfo;
import org.e1c.edt.ai.ui.AIUIModule.BaseModuleProvider;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.ui.editor.BslXtextDocument;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CurrentEditorModuleProvider
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
        var optionalModule = dispatcher.dispatch(() -> ui.getTextWidget()
            .flatMap(textWidget -> ui.getSourceViewer(textWidget)))
            .orElse(null)
            .flatMap(sourceViewer -> getModule(sourceViewer));

        if (optionalModule.isEmpty())
        {
            return baseResourceSetProvider.getModule(filePath, cancellationToken);
        }

        var module = optionalModule.get();
        var moduleFilePath = module.eResource().getURI().path();
        if (!filePath.equals(moduleFilePath))
        {
            return baseResourceSetProvider.getModule(filePath, cancellationToken);
        }

        var info = new ModuleInfo(module, null);
        return Optional.of(info);
    }

    private Optional<Module> getModule(SourceViewer sourceViewer)
    {
        var doc = sourceViewer.getDocument();
        if (doc instanceof BslXtextDocument)
        {
            IUnitOfWork<XtextResource, XtextResource> work = res -> res;
            var bslXtextDocument = ((BslXtextDocument)doc).readOnlyDataModel(work);
            for (var content : bslXtextDocument.getContents())
            {
                if (content instanceof Module)
                {
                    return Optional.of((Module)content);
                }
            }

        }

        return Optional.empty();
    }
}
