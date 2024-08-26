/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.e1c.edt.ai.AIModule;
import org.e1c.edt.ai.CodeCompletionActionHandler;
import org.e1c.edt.ai.CodeCompletionSession;
import org.e1c.edt.ai.ContextSettings;
import org.e1c.edt.ai.ICodeCompletionActionHandler;
import org.e1c.edt.ai.ICodeCompletionSession;
import org.e1c.edt.ai.IContextSettings;
import org.e1c.edt.ai.ICursorInfoProvider;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.context.ContextModule;
import org.e1c.edt.ai.ui.preferences.PreferenceStoreToSettingsStoreAdapter;
import org.eclipse.jface.preference.IPreferenceStore;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class AIUIModule
    extends AbstractModule
{
    public static final String PARAMETERS = "Parameters"; //$NON-NLS-1$
    public static final String URL = "URL"; //$NON-NLS-1$

    private Activator activator;

    public AIUIModule(Activator activator)
    {
        Preconditions.checkNotNull(activator);
        this.activator = activator;
    }

    @Override
    protected void configure()
    {
        // @formatter:off
        install(new AIModule());
        install(new ContextModule());
        bind(ILog.class).toInstance(activator);
        bind(IPluginVersion.class).toInstance(activator);
        bind(IDispatcher.class).to(Dispatcher.class).in(Singleton.class);
        bind(IPreferenceStore.class).toInstance(activator.getPreferenceStore());
        bind(ISettingsStore.class).to(PreferenceStoreToSettingsStoreAdapter.class).in(Singleton.class);
        bind(UI.class).in(Singleton.class);
        bind(IUI.class).to(UI.class);
        bind(IdeApiHandler.class).in(Singleton.class);
        bind(Chat.class).in(Singleton.class);
        bind(IChat.class).to(Chat.class);
        bind(IChatDialog.class).to(Chat.class);
        bind(IContextSettings.class).to(ContextSettings.class).in(Singleton.class);
        bind(new TypeLiteral<IAIContextProvider<Void>>() { /**/ }).to(AIContextProvider.class).in(Singleton.class);
        bind(new TypeLiteral<IAIContextProvider<AISourceContext>>() { /**/ }).annotatedWith(SourceMethodComments.class).to(AISourceMethodCommentsContextProvider.class).in(Singleton.class);
        bind(new TypeLiteral<IAIContextProvider<AISourceContext>>() { /**/ }).annotatedWith(SourceCodeSizeReducer.class).to(AISourceCodeSizeReducerContextProvider.class).in(Singleton.class);
        bind(new TypeLiteral<ISyntaxWalker<StringSerializerContext>>() { /**/ }).to(new TypeLiteral<BasicPathSyntaxWalker<StringSerializerContext>>() { /**/ }).in(Singleton.class);
        bind(new TypeLiteral<ISyntaxVisitor<StringSerializerContext>>() { /**/ }).to(StringSerializerVisitor.class).in(Singleton.class);
        bind(IUISettings.class).to(UISettings.class).in(Singleton.class);
        bind(new TypeLiteral<ICodeCompletionViewModel<CodeCompletionContext>>() { /**/ }).to(CodeCompletionViewModel.class).in(Singleton.class);
        bind(IHintPainter.class).to(HintPainter.class);
        bind(IHotKeys.class).to(HotKeys.class).in(Singleton.class);
        bind(IUserActions.class).to(UserActions.class).in(Singleton.class);
        bind(new TypeLiteral<ICodeCompletionSession<CodeCompletionContext>>() { /**/ }).to(new TypeLiteral<CodeCompletionSession<CodeCompletionContext>>() { /**/ });
        bind(new TypeLiteral<ICodeCompletionActionHandler<CodeCompletionContext>>() { /**/ }).to(new TypeLiteral<CodeCompletionActionHandler<CodeCompletionContext>>() { /**/ });
        bind(ICodePartsProvider.class).to(CodePartsProvider.class).in(Singleton.class);
        bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
        bind(IFinalCodeFeedbackViewModel.class).to(FinalCodeFeedbackViewModel.class).in(Singleton.class);
        bind(IFeedbackPainter.class).to(FeedbackPainter.class);
        bind(ICodeProvider.class).to(CodeProvider.class).in(Singleton.class);
        bind(IFeedbackDialog.class).to(FeedbackDialog.class);
        bind(IIssueFeedbackViewModel.class).to(IssueFeedbackViewModel.class);
        // @formatter:on
    }

    @Qualifier
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    public @interface SourceCodeSizeReducer
    {
        //
    }

    @Qualifier
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    public @interface SourceMethodComments
    {
        //
    }
}