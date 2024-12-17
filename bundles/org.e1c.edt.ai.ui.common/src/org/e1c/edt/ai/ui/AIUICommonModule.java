/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.AIModule;
import org.e1c.edt.ai.CodeCompletionActionHandler;
import org.e1c.edt.ai.CodeCompletionSession;
import org.e1c.edt.ai.ICodeCompletionActionHandler;
import org.e1c.edt.ai.ICodeCompletionSession;
import org.e1c.edt.ai.ICursorInfoProvider;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.ui.handlers.FixDialog;
import org.e1c.edt.ai.ui.handlers.IFixDialog;
import org.e1c.edt.ai.ui.preferences.PreferenceStoreToSettingsStoreAdapter;

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
        bind(IdeApiHandler.class).in(Singleton.class);
        bind(Chat.class).in(Singleton.class);
        bind(IChat.class).to(Chat.class);
        bind(IChatDialog.class).to(Chat.class);
        bind(IAIContextProvider.class).to(AIContextProvider.class).in(Singleton.class);
        bind(IUISettings.class).to(UISettings.class).in(Singleton.class);
        bind(new TypeLiteral<ICodeCompletionViewModel<CodeCompletionContext>>() { /**/ }).to(CodeCompletionViewModel.class).in(Singleton.class);
        bind(IHintPainter.class).to(HintPainter.class);
        bind(IHotKeys.class).to(HotKeys.class).in(Singleton.class);
        bind(IUserActions.class).to(UserActions.class).in(Singleton.class);
        bind(new TypeLiteral<ICodeCompletionSession<CodeCompletionContext>>() { /**/ }).to(new TypeLiteral<CodeCompletionSession<CodeCompletionContext>>() { /**/ });
        bind(new TypeLiteral<ICodeCompletionActionHandler<CodeCompletionContext>>() { /**/ }).to(new TypeLiteral<CodeCompletionActionHandler<CodeCompletionContext>>() { /**/ });
        bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
        bind(IFinalCodeFeedbackViewModel.class).to(FinalCodeFeedbackViewModel.class).in(Singleton.class);
        bind(IFeedbackPainter.class).to(FeedbackPainter.class);
        bind(IFeedbackDialog.class).to(FeedbackDialog.class);
        bind(IIssueFeedbackViewModel.class).to(IssueFeedbackViewModel.class);
        bind(IFixDialog.class).to(FixDialog.class).in(Singleton.class);
        bind(IContentProvider.class).to(ContentProvider.class).in(Singleton.class);
        bind(IJavaScript.class).to(JavaScript.class).in(Singleton.class);
        bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
        bind(IGlobalContextViewModel.class).to(GlobalContextViewModel.class).in(Singleton.class);
        // @formatter:on
    }
}