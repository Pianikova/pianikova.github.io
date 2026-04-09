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
        toolBinder.addBinding().to(ExecuteCommandMcpTool.class);
        toolBinder.addBinding().to(ReadMcpTool.class);
        toolBinder.addBinding().to(FindMcpTool.class);
        toolBinder.addBinding().to(SearchTextMcpTool.class);
        toolBinder.addBinding().to(SearchFilesMcpTool.class);
        toolBinder.addBinding().to(GlobMcpTool.class);
        toolBinder.addBinding().to(ListMcpTool.class);
        // toolBinder.addBinding().to(GitMcpTool.class);
        toolBinder.addBinding().to(JGitMcpTool.class);
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
        toolBinder.addBinding().to(JShellSessionMcpTool.class);
        toolBinder.addBinding().to(JShellMcpTool.class);
        bind(IProcessRunner.class).to(ProcessRunner.class).in(Singleton.class);
        bind(ICancellationProgressMonitor.class).to(CancellationProgressMonitor.class);
        bind(IBuildWaiter.class).to(BuildWaiter.class).in(Singleton.class);
        bind(IMcpToolsCallMessageFactory.class).to(McpToolsCallMessageFactory.class).in(Singleton.class);
        bind(IMarkdownUtils.class).to(com.e1c.edt.ai.MarkdownUtils.class).in(Singleton.class);
        bind(ILocalHistoryUtils.class).to(LocalHistoryUtils.class).in(Singleton.class);
        bind(IPatternMatcher.class).to(PatternMatcher.class).in(Singleton.class);
        bind(IJShellSessionManager.class).to(JShellSessionManager.class).in(Singleton.class);
        bind(ITreeBuilder.class).to(TreeBuilder.class);
        bind(IJShellClassPathProvider.class).to(JShellClassPathProvider.class).in(Singleton.class);
        bind(IJShellClassPathProvider.class).to(JShellClassPathProvider.class).in(Singleton.class);
        bind(IRestrictedTypesProvider.class).to(RestrictedTypesProvider.class).in(Singleton.class);
        bind(IRestrictedTypesValidator.class).to(RestrictedTypesValidator.class).in(Singleton.class);
        bind(IReplacements.class).to(Replacements.class).in(Singleton.class);
        bind(IContentReplacer.class).to(ContentReplacer.class).in(Singleton.class);
        bind(IJGitCommonHelper.class).to(JGitCommonHelper.class).in(Singleton.class);

        // Replacement strategies
        var replacementStrategyBinder = Multibinder.newSetBinder(binder(), IReplacementStrategy.class);
        replacementStrategyBinder.addBinding().to(SimpleReplacer.class);
        replacementStrategyBinder.addBinding().to(LineTrimmedReplacer.class);
        replacementStrategyBinder.addBinding().to(BlockAnchorReplacer.class);
        replacementStrategyBinder.addBinding().to(WhitespaceNormalizedReplacer.class);
        replacementStrategyBinder.addBinding().to(IndentationFlexibleReplacer.class);
        replacementStrategyBinder.addBinding().to(EscapeNormalizedReplacer.class);
        replacementStrategyBinder.addBinding().to(TrimmedBoundaryReplacer.class);
        replacementStrategyBinder.addBinding().to(ContextAwareReplacer.class);
        replacementStrategyBinder.addBinding().to(MultiOccurrenceReplacer.class);

        // Markers providers
        var markersProviderBinder = Multibinder.newSetBinder(binder(), IMarkersProvider.class);
        markersProviderBinder.addBinding().to(CommonMarkersProvider.class);

        // JShell binding providers
        var jshellBindingProviderBinder = Multibinder.newSetBinder(binder(), IJShellBindingProvider.class);
        jshellBindingProviderBinder.addBinding().to(EclipsePlatformBindingProvider.class);

        // JGit command implementations
        var jgitCommandBinder = Multibinder.newSetBinder(binder(), IJGitCommand.class);
        jgitCommandBinder.addBinding().to(JGitAdd.class);
        jgitCommandBinder.addBinding().to(JGitApply.class);
        jgitCommandBinder.addBinding().to(JGitBlame.class);
        jgitCommandBinder.addBinding().to(JGitBranch.class);
        jgitCommandBinder.addBinding().to(JGitCheckout.class);
        jgitCommandBinder.addBinding().to(JGitCherryPick.class);
        jgitCommandBinder.addBinding().to(JGitClean.class);
        jgitCommandBinder.addBinding().to(JGitClone.class); //
        jgitCommandBinder.addBinding().to(JGitCommit.class);
        jgitCommandBinder.addBinding().to(JGitConfig.class);
        jgitCommandBinder.addBinding().to(JGitDescribe.class); //
        jgitCommandBinder.addBinding().to(JGitDiff.class);
        jgitCommandBinder.addBinding().to(JGitFetch.class);
        jgitCommandBinder.addBinding().to(JGitLog.class);
        jgitCommandBinder.addBinding().to(JGitLsFiles.class); //
        jgitCommandBinder.addBinding().to(JGitMerge.class); //
        jgitCommandBinder.addBinding().to(JGitMv.class);
        jgitCommandBinder.addBinding().to(JGitPull.class);
        jgitCommandBinder.addBinding().to(JGitPush.class);
        jgitCommandBinder.addBinding().to(JGitRebase.class); //
        jgitCommandBinder.addBinding().to(JGitRemote.class); //
        jgitCommandBinder.addBinding().to(JGitReset.class);
        jgitCommandBinder.addBinding().to(JGitRevert.class);
        jgitCommandBinder.addBinding().to(JGitRm.class);
        jgitCommandBinder.addBinding().to(JGitShow.class);
        jgitCommandBinder.addBinding().to(JGitShowBranch.class);
        jgitCommandBinder.addBinding().to(JGitStash.class);
        jgitCommandBinder.addBinding().to(JGitStatus.class);
        jgitCommandBinder.addBinding().to(JGitTag.class);
    }
}
