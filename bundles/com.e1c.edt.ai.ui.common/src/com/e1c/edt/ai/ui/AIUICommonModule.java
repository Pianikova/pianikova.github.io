/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIModule;
import com.e1c.edt.ai.CodeCompletionActionHandler;
import com.e1c.edt.ai.CodeCompletionSession;
import com.e1c.edt.ai.ICodeCompletionActionHandler;
import com.e1c.edt.ai.ICodeCompletionSession;
import com.e1c.edt.ai.ICursorInfoProvider;
import com.e1c.edt.ai.IGlobalContextManager;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.ui.handlers.CodeTools;
import com.e1c.edt.ai.ui.handlers.FixDialog;
import com.e1c.edt.ai.ui.handlers.ICodeTools;
import com.e1c.edt.ai.ui.handlers.IFixDialog;
import com.e1c.edt.ai.ui.preferences.PreferenceStoreToSettingsStoreAdapter;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class AIUICommonModule
    extends AbstractModule
{
    public static final String PARAMETERS = "Parameters"; //$NON-NLS-1$
    public static final String URL = "URL"; //$NON-NLS-1$

    @Override
    protected void configure()
    {
        // @formatter:off
        install(new AIModule());
        bind(IDispatcher.class).to(Dispatcher.class).in(Singleton.class);
        bind(ISettingsStore.class).to(PreferenceStoreToSettingsStoreAdapter.class).in(Singleton.class);
        bind(UI.class).in(Singleton.class);
        bind(IUI.class).to(UI.class);
        bind(IUIInitializer.class).to(UI.class);
        bind(ClipboardManager.class).in(Singleton.class);
        bind(IClipboardManager.class).to(ClipboardManager.class);
        bind(IClipboard.class).to(ClipboardManager.class);
        bind(IdeApiHandler.class).in(Singleton.class);
        bind(Chat.class).in(Singleton.class);
        bind(IChat.class).to(Chat.class);
        bind(IChatDialog.class).to(Chat.class);
        bind(IAIContextProvider.class).to(AIContextProvider.class).in(Singleton.class);
        bind(IUISettings.class).to(UISettings.class).in(Singleton.class);
        bind(new TypeLiteral<ICodeCompletionViewModel<CodeCompletionContext>>() { /**/ }).to(CodeCompletionViewModel.class).in(Singleton.class);
        bind(IHintPainter.class).to(HintPainter.class).in(Singleton.class);
        bind(IVerticalRulerPainter.class).to(VerticalRulerPainter.class).in(Singleton.class);
        bind(IHotKeys.class).to(HotKeys.class).in(Singleton.class);
        bind(IUserActions.class).to(UserActions.class).in(Singleton.class);
        bind(new TypeLiteral<ICodeCompletionSession<CodeCompletionContext>>() { /**/ }).to(new TypeLiteral<CodeCompletionSession<CodeCompletionContext>>() { /**/ });
        bind(new TypeLiteral<ICodeCompletionActionHandler<CodeCompletionContext>>() { /**/ }).to(new TypeLiteral<CodeCompletionActionHandler<CodeCompletionContext>>() { /**/ });
        bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
        bind(IFeedbackDialog.class).to(FeedbackDialog.class);
        bind(IIssueFeedbackViewModel.class).to(IssueFeedbackViewModel.class);
        bind(IFixDialog.class).to(FixDialog.class).in(Singleton.class);
        bind(IContentProvider.class).to(ContentProvider.class).in(Singleton.class);
        bind(IJavaScript.class).to(JavaScript.class).in(Singleton.class);
        bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
        bind(IGlobalContextManager.class).to(GlobalContextManager.class).in(Singleton.class);
        bind(ISyntaxVaidator.class).to(SyntaxVaidator.class).in(Singleton.class);
        bind(IGlobalContextTracker.class).to(GlobalContextTracker.class).in(Singleton.class);
        bind(IProjectTrackingWorkflow.class).to(ProjectTrackingWorkflow.class);
        bind(IGlobalContextSync.class).to(GlobalContextSync.class).in(Singleton.class);
        bind(IProposalsProvider.class).to(ProposalsProvider.class).in(Singleton.class);
        bind(ICodeParser.class).to(CodeParser.class).in(Singleton.class);
        bind(TextWidgetInfo.class).in(Singleton.class);
        bind(ITextWidgetInfoUpdater.class).to(TextWidgetInfo.class);
        bind(ITextWidgetInfoProvider.class).to(TextWidgetInfo.class);
        bind(ICodeTools.class).to(CodeTools.class).in(Singleton.class);
        bind(IVerticalRulerManager.class).to(VerticalRulerManager.class).in(Singleton.class);
        bind(IGCTools.class).to(GCTools.class).in(Singleton.class);
        bind(IFileScaner.class).to(FileScaner.class).in(Singleton.class);
        // @formatter:on
    }
}