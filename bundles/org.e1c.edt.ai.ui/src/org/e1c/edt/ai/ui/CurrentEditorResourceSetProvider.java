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

import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CurrentEditorResourceSetProvider
    implements IResourceSetProvider
{
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final ICodeProvider codeProvider;
    private final IResourceSetProvider baseResourceSetProvider;
    private final IResourceLookup resourceLookup;

    @Inject
    public CurrentEditorResourceSetProvider(IUI ui, IDispatcher dispatcher, ICodeProvider codeProvider,
        @BaseResourceSetProvider IResourceSetProvider baseResourceSetProvider, IResourceLookup resourceLookup)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(codeProvider);
        Preconditions.checkNotNull(baseResourceSetProvider);
        Preconditions.checkNotNull(resourceLookup);
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.codeProvider = codeProvider;
        this.baseResourceSetProvider = baseResourceSetProvider;
        this.resourceLookup = resourceLookup;
    }

    @Override
    public IProject[] getProjects()
    {
        return getParseResult().flatMap(parseResult -> getProject(parseResult)).map(project -> {
            var projects = new IProject[1];
            projects[0] = project;
            return projects;
        }).orElseGet(() -> new IProject[0]);
    }

    @Override
    public ResourceSet getResourceSet(IProject project)
    {
        Preconditions.checkNotNull(project);
        return getParseResult().flatMap(parseResult -> getResourceSet(parseResult))
            .orElseGet(() -> baseResourceSetProvider.getResourceSet(project));
    }

    private Optional<IParseResult> getParseResult()
    {
        return dispatcher
            .dispatch(() -> ui.getTextWidget().flatMap(textWidget -> ui.getSourceViewer(textWidget)).orElse(null))
            .flatMap(sourceViewer -> codeProvider.getParseResult(sourceViewer));
    }

    private Optional<IProject> getProject(IParseResult parseResult)
    {
        var root = parseResult.getRootASTElement();
        if (root == null)
        {
            return Optional.empty();
        }

        var project = resourceLookup.getDtProject(root);
        if (project == null)
        {
            return Optional.empty();
        }

        return Optional.ofNullable(project.getWorkspaceProject());
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
