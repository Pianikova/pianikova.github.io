/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMarkersProvider;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpTools;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.McpTools;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class MCPToolsModule
    extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(IMcpTools.class).to(McpTools.class).in(Singleton.class);
        var toolBinder = Multibinder.newSetBinder(binder(), IMcpTool.class);
        toolBinder.addBinding().to(ExecuteMcpTool.class);
        toolBinder.addBinding().to(GitMcpTool.class);
        toolBinder.addBinding().to(GetProjectsMcpTool.class);
        toolBinder.addBinding().to(GetCommandCategoriesMcpTool.class);
        toolBinder.addBinding().to(GetCommandsMcpTool.class);
        // toolBinder.addBinding().to(ExecuteCommandMcpTool.class);
        toolBinder.addBinding().to(ReadMcpTool.class);
        toolBinder.addBinding().to(FindMcpTool.class);
        toolBinder.addBinding().to(SearchTextMcpTool.class);
        // toolBinder.addBinding().to(SearchFilesMcpTool.class);
        toolBinder.addBinding().to(GlobMcpTool.class);
        // toolBinder.addBinding().to(GitCommitsMcpTool.class);
        // toolBinder.addBinding().to(GitDiffMcpTool.class);
        toolBinder.addBinding().to(LocalHistoryMcpTool.class);
        toolBinder.addBinding().to(LocalChangesMcpTool.class);
        toolBinder.addBinding().to(NavigationHistoryMcpTool.class);
        toolBinder.addBinding().to(WriteMcpTool.class);
        toolBinder.addBinding().to(EditMcpTool.class);
        toolBinder.addBinding().to(DeleteMcpTool.class);
        toolBinder.addBinding().to(GetMarkersMcpTool.class);
        toolBinder.addBinding().to(SetMarkersMcpTool.class);
        toolBinder.addBinding().to(DeleteMarkersMcpTool.class);
        toolBinder.addBinding().to(JShellMcpTool.class);
        bind(IProcessRunner.class).to(ProcessRunner.class).in(Singleton.class);
        bind(ICancellationProgressMonitor.class).to(CancellationProgressMonitor.class);
        bind(IBuildWaiter.class).to(BuildWaiter.class).in(Singleton.class);
        bind(IMcpToolsCallMessageFactory.class).to(McpToolsCallMessageFactory.class).in(Singleton.class);
        bind(IMarkdownUtils.class).to(com.e1c.edt.ai.MarkdownUtils.class).in(Singleton.class);
        bind(ILocalHistoryUtils.class).to(LocalHistoryUtils.class).in(Singleton.class);
        bind(IPatternMatcher.class).to(PatternMatcher.class).in(Singleton.class);
        bind(IJShellSessionManager.class).to(JShellSessionManager.class).in(Singleton.class);
        bind(IRestrictedTypesProvider.class).to(RestrictedTypesProvider.class).in(Singleton.class);
        bind(IRestrictedTypesValidator.class).to(RestrictedTypesValidator.class).in(Singleton.class);

        // Markers providers
        var markersProviderBinder = Multibinder.newSetBinder(binder(), IMarkersProvider.class);
        markersProviderBinder.addBinding().to(CommonMarkersProvider.class);

        // JShell binding providers
        var jshellBindingProviderBinder = Multibinder.newSetBinder(binder(), IJShellBindingProvider.class);
        jshellBindingProviderBinder.addBinding().to(EclipsePlatformBindingProvider.class);
        jshellBindingProviderBinder.addBinding().to(ExampleUtilsBindingProvider.class);
    }
}
