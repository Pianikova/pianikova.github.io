/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.context.IResourceSetProvider;
import org.e1c.edt.ai.ui.AIUIModule.BaseResourceSetProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.parser.IParseResult;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CurrentEditorResourceSetProvider
    implements IResourceSetProvider
{
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final ICodeProvider codeProvider;
    private final IResourceSetProvider baseResourceSetProvider;

    @Inject
    public CurrentEditorResourceSetProvider(IUI ui, IDispatcher dispatcher, ICodeProvider codeProvider,
        @BaseResourceSetProvider IResourceSetProvider baseResourceSetProvider)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(codeProvider);
        Preconditions.checkNotNull(baseResourceSetProvider);
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.codeProvider = codeProvider;
        this.baseResourceSetProvider = baseResourceSetProvider;
    }

    @Override
    public ResourceSet getResourceSet(IProject project)
    {
        Preconditions.checkNotNull(project);
        return dispatcher
            .dispatch(() -> ui.getTextWidget().flatMap(textWidget -> ui.getSourceViewer(textWidget)).orElse(null))
            .flatMap(sourceViewer -> codeProvider.getParseResult(sourceViewer))
            .flatMap(parseResult -> getResourceSet(parseResult))
            .orElseGet(() -> baseResourceSetProvider.getResourceSet(project));
    }

    private Optional<ResourceSet> getResourceSet(IParseResult parseResult)
    {
        var astRoot = parseResult.getRootASTElement();
        if (astRoot == null)
        {
            return Optional.empty();
        }

        var resource = astRoot.eResource();
        if (resource == null)
        {
            return Optional.empty();
        }

        return Optional.ofNullable(resource.getResourceSet());
    }
}
